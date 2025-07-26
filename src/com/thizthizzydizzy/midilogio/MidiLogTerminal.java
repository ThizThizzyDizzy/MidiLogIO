package com.thizthizzydizzy.midilogio;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
public class MidiLogTerminal{
    public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException, InterruptedException, IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Type \"editor\" to use the Unity Editor, or just press Enter to read from VRChat");
        MidiLogIO io;
        try{
            io = new MidiLogIO(reader.readLine().equals("editor")?MidiLogIO.LOG_UNITY_EDITOR:MidiLogIO.LOG_VRCHAT);
        }catch(MidiUnavailableException ex){
            System.out.println("List of MIDI devices:");
            for(MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()){
                System.out.println(info.getName()+": "+info.getDescription()+" ("+info.getVendor()+" "+info.getVersion()+")");
            }
            throw ex;
        }
        io.start();

        System.out.println("\nFinished initalization with "+io.connections.size()+" active connections.\n\n--------\n");
        String line;
        while((line = reader.readLine())!=null){
            String ln = line;
            io.connections.forEach((c) -> {
                try{
                    c.send(ln+"\n");
                }catch(InvalidMidiDataException ex){
                    Logger.getLogger(MidiLogTerminal.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
        reader.close();
        io.close();
    }
}
