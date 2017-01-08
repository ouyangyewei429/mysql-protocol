package com.seaboat.mysql.protocol;

import java.io.IOException;
import java.io.OutputStream;

import com.seaboat.mysql.protocol.util.BufferUtil;
import com.seaboat.mysql.protocol.util.StreamUtil;

/**
 * 
 * @author seaboat
 * @date 2016-09-25
 * @version 1.0
 * <pre><b>email: </b>849586227@qq.com</pre>
 * <pre><b>blog: </b>http://blog.csdn.net/wangyangzhizhou</pre>
 * <p>AuthPacket .</p>
 */
public class AuthPacket extends MySQLPacket {
	private static final byte[] FILLER = new byte[23];

	public long clientFlags;
	public long maxPacketSize;
	public int charsetIndex;
	public byte[] extra;
	public String user;
	public byte[] password;
	public String database;

	public void read(byte[] data) {
		MySQLMessage mm = new MySQLMessage(data);
		packetLength = mm.readUB3();
		packetId = mm.read();
		clientFlags = mm.readUB4();
		maxPacketSize = mm.readUB4();
		charsetIndex = (mm.read() & 0xff);
		int current = mm.position();
		int len = (int) mm.readLength();
		if (len > 0 && len < FILLER.length) {
			byte[] ab = new byte[len];
			System.arraycopy(mm.bytes(), mm.position(), ab, 0, len);
			this.extra = ab;
		}
		mm.position(current + FILLER.length);
		user = mm.readStringWithNull();
		password = mm.readBytesWithLength();
		if (((clientFlags & Capabilities.CLIENT_CONNECT_WITH_DB) != 0)
				&& mm.hasRemaining()) {
			database = mm.readStringWithNull();
		}
	}

	public void write(OutputStream out) throws IOException {
		StreamUtil.writeUB3(out, calcPacketSize());
		StreamUtil.write(out, packetId);
		StreamUtil.writeUB4(out, clientFlags);
		StreamUtil.writeUB4(out, maxPacketSize);
		StreamUtil.write(out, (byte) charsetIndex);
		out.write(FILLER);
		if (user == null) {
			StreamUtil.write(out, (byte) 0);
		} else {
			StreamUtil.writeWithNull(out, user.getBytes());
		}
		if (password == null) {
			StreamUtil.write(out, (byte) 0);
		} else {
			StreamUtil.writeWithLength(out, password);
		}
		if (database == null) {
			StreamUtil.write(out, (byte) 0);
		} else {
			StreamUtil.writeWithNull(out, database.getBytes());
		}
	}

	@Override
	public int calcPacketSize() {
		int size = 32;// 4+4+1+23;
		size += (user == null) ? 1 : user.length() + 1;
		size += (password == null) ? 1 : BufferUtil.getLength(password);
		size += (database == null) ? 1 : database.length() + 1;
		return size;
	}

	@Override
	protected String getPacketInfo() {
		return "MySQL Authentication Packet";
	}

}
