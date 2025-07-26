package com.thizthizzydizzy.midilogio.connection;
import java.util.UUID;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
public class HalfConnection extends Connection{
    private final int channel;
    public HalfConnection(UUID uid, String settings){
        super(uid, 1);
        channel = Integer.parseInt(settings);
        if(channel<0||channel>15)throw new IllegalArgumentException("Channel must be between 0 and 15!");
    }

    @Override
    protected void sendFrame(Receiver receiver, byte[] bytes) throws InvalidMidiDataException{
        int halfMessage = Byte.toUnsignedInt(bytes[0]);

        int number = (halfMessage>>7&0b1)+118;
        int value = halfMessage&0b1111111;
        
        System.out.println("["+uid.toString()+"] Sending "+Integer.toHexString(halfMessage));

        receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, number, value), -1);
    }
    @Override
    public int[] getUsedChannels(){
        return new int[]{channel};
    }
}
