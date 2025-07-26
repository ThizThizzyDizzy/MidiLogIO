package com.thizthizzydizzy.midilogio;
import com.thizthizzydizzy.midilogio.connection.Connection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
public class MidiLogIO{
    public static final String LOG_VRCHAT = "C:\\Users\\Thiz\\AppData\\LocalLow\\VRChat\\VRChat";
    public static final String LOG_UNITY_EDITOR = System.getenv("LOCALAPPDATA")+"\\Unity\\Editor\\Editor.log";
    private final String logPath;
    private final MidiDevice device;
    public Receiver receiver;
    private LogfileParser parser;
    public final ArrayList<Connection> connections = new ArrayList<>();
    private final ArrayList<Consumer<Connection>> connectionListeners = new ArrayList<>();
    public MidiLogIO() throws MidiUnavailableException{
        this(LOG_VRCHAT);
    }
    public MidiLogIO(String logPath) throws MidiUnavailableException{
        this(logPath, getDefaultDevice());
    }
    public MidiLogIO(String logPath, MidiDevice.Info device) throws MidiUnavailableException{
        this.logPath = logPath;
        this.device = MidiSystem.getMidiDevice(device);
    }
    public void start() throws MidiUnavailableException, IOException{
        System.out.println("[MidiLogIO] Using MIDI Device: "+device.getDeviceInfo().toString());
        device.open();
        receiver = device.getReceiver();

        parser = new LogfileParser(this, logPath);
        parser.start();
        for(var conn : connections){
            connectionListeners.forEach((l) -> l.accept(conn));
        }
    }
    public void close(){
        parser.close();
        receiver.close();
        device.close();
    }
    public static MidiDevice.Info getDefaultDevice() throws MidiUnavailableException{
        MidiDevice.Info device = null;
        for(MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()){
            if(info.getName().contains("loopMIDI")&&info.getDescription().contains("External")){
                device = info;
            }
        }
        if(device==null)throw new MidiUnavailableException("Could not find loopMIDI device! Please specify the device you want to use.");
        return device;
    }
    public void addConnectionListener(Consumer<Connection> listener){
        connectionListeners.add(listener);
    }
    void addConnection(Connection connection){
        connections.add(connection);
        if(parser.running)connectionListeners.forEach((l) -> l.accept(connection));
    }
}
