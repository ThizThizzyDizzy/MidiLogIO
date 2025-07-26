package com.thizthizzydizzy.midilogio;
import com.thizthizzydizzy.midilogio.connection.ConnectionManager;
import com.thizthizzydizzy.midilogio.connection.Connection;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
public class LogfileParser{
    private MidiLogIO io;
    private final File source;
    boolean running;
    public LogfileParser(MidiLogIO io, String source){
        this(new File(source));
        this.io = io;
    }
    public LogfileParser(File source){
        this.source = source;
    }
    private ArrayList<File> getLogFiles(File source){
        ArrayList<File> files = new ArrayList<>();
        files.addAll(Arrays.asList(source.listFiles((f) -> f.getName().matches("output_log_\\d\\d\\d\\d-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d.txt"))));
        files.sort((f1, f2) -> (int)((f1.lastModified()-f2.lastModified())/1_000));
        return files;
    }
    public void start() throws IOException{
        ArrayList<File> files = new ArrayList<>();
        if(source.isFile())files.add(source);
        if(source.isDirectory()){
            files.clear();
            files.addAll(getLogFiles(source));
        }
        for(File f : files){
            for(String line : Files.readAllLines(f.toPath())){
                parseInit(line);
            }
        }
        running = true;
        Thread listener = new Thread(() -> {
            File currentSource = source;
            if(source.isDirectory()){
                currentSource = files.get(files.size()-1);
            }
            long position = currentSource.length();
            int counter = 0;
            while(running){
                try{
                    if(source.isDirectory()&&counter++%15==0){
                        files.clear();
                        files.addAll(getLogFiles(source));
                        var newSource = files.get(files.size()-1);
                        if(!currentSource.equals(newSource)){
                            currentSource = newSource;
                            position = 0;
                        }
                    }

                    RandomAccessFile raf = new RandomAccessFile(currentSource, "r");
                    raf.seek(position);
                    String line;
                    while((line = raf.readLine())!=null){
                        if(!line.contains("[MidiLogIO"))continue;
                        line = line.substring(line.indexOf("[MidiLogIO"));
                        parseInit(line);
                        if(line.matches("\\[MidiLogIO\\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\] DATA - .*")){
                            String uid = line.substring(11, 36+11);
                            for(var connection : io.connections){
                                if(connection.uid.toString().equals(uid)){
                                    String hex = line.split(" ", 4)[3];
                                    byte[] bytes = new byte[hex.length()/2];
                                    for(int i = 0; i<bytes.length; i++){
                                        bytes[i] = Byte.parseByte(hex.substring(i*2, i*2+2), 16);
                                    }
                                    connection.receive(bytes);
                                }
                            }
                        }
                        position = raf.getFilePointer();
                    }
                    Thread.sleep(1000);
                }catch(IOException|InterruptedException ex){
                    Logger.getLogger(LogfileParser.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                }
            }
        });
        listener.start();
    }
    private void parseInit(String line){
        if(!line.contains("[MidiLogIO"))return;
        line = line.substring(line.indexOf("[MidiLogIO"));
        if(line.matches("\\[MidiLogIO\\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\] INIT - .*")){
            String uid = line.substring(11, 36+11);
            String[] capabilities = line.split("\t");
            String definition = capabilities[0].split(" ", 4)[3];
            try{
                var connection = ConnectionManager.create(io, UUID.fromString(uid), definition);
                for(int i = 1; i<capabilities.length; i++)connection.capabilities.add(capabilities[i]);
                var channels = connection.getUsedChannels();
                ArrayList<Connection> invalidConnections = new ArrayList<>();
                CON:
                for(var conn : io.connections){
                    for(int a : conn.getUsedChannels()){
                        for(int b : channels){
                            if(a==b){
                                invalidConnections.add(conn);
                                continue CON;
                            }
                        }
                    }
                }
                if(!invalidConnections.isEmpty())System.out.println("Removing "+invalidConnections.size()+" invalid connections...");
                invalidConnections.forEach((c) -> c.valid = false);
                io.connections.removeAll(invalidConnections);
                io.addConnection(connection);
            }catch(Exception ex){
                System.out.println("Skipping invalid init: "+line);
                return;
            }
            System.out.println(line+" "+uid);
        }
    }
    public void close(){
        running = false;
    }
}
