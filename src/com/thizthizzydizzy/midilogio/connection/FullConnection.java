package com.thizthizzydizzy.midilogio.connection;
import java.util.UUID;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
public class FullConnection extends Connection{
    private final int[] channels = new int[2];
    public FullConnection(UUID uid, String settings){
        super(uid, 2);
        String[] channelStrs = settings.split(" ");
        if(channelStrs.length!=channels.length)throw new IllegalArgumentException("Malformed settings for Full Connection! Must be "+channels.length+" channel numbers between 0 and 15.");
        for(int i = 0; i<channels.length; i++){
            channels[i] = Integer.parseInt(channelStrs[i]);
            if(channels[i]<0||channels[i]>15)throw new IllegalArgumentException("Invalid channel: "+i);
        }
    }

    @Override
    protected void sendFrame(Receiver receiver, byte[] bytes) throws InvalidMidiDataException{
        if(bytes.length==1){
            // Send a single byte as a Half message
            int halfMessage = Byte.toUnsignedInt(bytes[0]);

            int number = (halfMessage>>7&0b1)+118;
            int value = halfMessage&0b1111111;

            System.out.println("["+uid.toString()+"] Sending "+Integer.toHexString(halfMessage));

            receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channels[channels.length-1], number, value), -1);
            return;
        }

        int fullMessage = (Byte.toUnsignedInt(bytes[0])<<8)+Byte.toUnsignedInt(bytes[1]);

        int messageType = (fullMessage>>15&0b1)==0?ShortMessage.NOTE_OFF:ShortMessage.NOTE_ON;
        int channelIndex = fullMessage>>14&0b1;
        int number = fullMessage>>7&0b1111111;
        int value = fullMessage&0b1111111;

        System.out.println("["+uid.toString()+"] Sending "+Integer.toHexString(fullMessage));

        receiver.send(new ShortMessage(messageType, channels[channelIndex], number, value), -1);
    }
    @Override
    public int[] getUsedChannels(){
        return channels;
    }
}
