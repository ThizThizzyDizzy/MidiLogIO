package com.thizthizzydizzy.midilogio.connection;
import com.thizthizzydizzy.midilogio.MidiLogIO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
public abstract class Connection{
    public MidiLogIO io;
    public final UUID uid;
    private final int bytesPerFrame;
    public int speed = 200;
    public HashSet<String> capabilities = new HashSet<>();
    public boolean valid = true;
    public ArrayList<Consumer<byte[]>> listeners = new ArrayList<>();
    public Connection(UUID uid, int bytesPerFrame){
        this.uid = uid;
        this.bytesPerFrame = bytesPerFrame;
    }
    protected abstract void sendFrame(Receiver receiver, byte[] bytes) throws InvalidMidiDataException;
    public void send(Receiver receiver, byte[] data) throws InvalidMidiDataException{
        if(!valid)return;
        synchronized(receiver){
            for(int i = 0; i<data.length; i += bytesPerFrame){
                byte[] frame = new byte[Math.min(bytesPerFrame, data.length-i)];
                for(int j = 0; j<frame.length; j++)frame[j] = data[i+j];
                sendFrame(receiver, frame);

                long waitNanos = (1_000_000_000L/speed);
                long waitMillis = waitNanos/1_000_000;
                int justNanos = (int)(waitNanos-waitMillis*1_000_000);
                try{
                    receiver.wait(waitMillis, justNanos);
                }catch(InterruptedException ex){
                }
            }
        }
    }
    public void sendLine(String data) throws InvalidMidiDataException{
        send(data+"\n");
    }
    public void send(String data) throws InvalidMidiDataException{
        send(io.receiver, data.getBytes());
    }
    public abstract int[] getUsedChannels();
    public void addListener(Consumer<byte[]> listener){
        listeners.add(listener);
    }
    public void addLineListener(Consumer<String> listener){
        listeners.add(new Consumer<byte[]>(){
            String line = "";
            @Override
            public void accept(byte[] t){
                line += new String(t);
                while(line.contains("\n")){
                    String[] lines = line.split("\n", 2);
                    listener.accept(lines[0]);
                    line = lines[1];
                }
            }
        });
    }
    public void receive(byte[] bytes){
        if(!valid)return;
        listeners.forEach((l) -> l.accept(bytes));
    }
}
