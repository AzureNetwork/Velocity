package com.velocitypowered.proxy.protocol.netty;

import com.google.common.base.Preconditions;
import com.velocitypowered.proxy.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf> {
    private StateRegistry state;
    private final ProtocolConstants.Direction direction;
    private StateRegistry.PacketRegistry.ProtocolVersion protocolVersion;

    public MinecraftDecoder(ProtocolConstants.Direction direction) {
        this.state = StateRegistry.HANDSHAKE;
        this.direction = Preconditions.checkNotNull(direction, "direction");
        this.setProtocolVersion(ProtocolConstants.MINECRAFT_1_12);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (!msg.isReadable()) {
            return;
        }

        ByteBuf slice = msg.slice().retain();

        int packetId = ProtocolUtils.readVarInt(msg);
        MinecraftPacket packet = this.protocolVersion.createPacket(packetId);
        if (packet == null) {
            msg.skipBytes(msg.readableBytes());
            out.add(new PacketWrapper(null, slice));
        } else {
            try {
                packet.decode(msg, direction, protocolVersion.id);
            } catch (Exception e) {
                throw new CorruptedFrameException("Error decoding " + packet.getClass() + " Direction " + direction
                        + " Protocol " + protocolVersion + " State " + state + " ID " + Integer.toHexString(packetId), e);
            }
            out.add(new PacketWrapper(packet, slice));
        }
    }

    public StateRegistry.PacketRegistry.ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = (this.direction == ProtocolConstants.Direction.CLIENTBOUND ? this.state.CLIENTBOUND : this.state.SERVERBOUND).getVersion(protocolVersion);
    }

    public StateRegistry getState() {
        return state;
    }

    public void setState(StateRegistry state) {
        this.state = state;
        this.setProtocolVersion(protocolVersion.id);
    }

    public ProtocolConstants.Direction getDirection() {
        return direction;
    }
}
