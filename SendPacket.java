import java.io.Serializable;

public class SendPacket implements Serializable{
	
	
	int seqnumber;
	int checksum;
	int data;
	byte[] dataValue;
	
	
	public byte[] getDataValue() {
		return dataValue;
	}
	public void setDataValue(byte[] dataValue) {
		this.dataValue = dataValue;
	}
	public int getSeqnumber() {
		return seqnumber;
	}
	public void setSeqnumber(int seqnumber) {
		this.seqnumber = seqnumber;
	}
	public int getChecksum() {
		return checksum;
	}
	public SendPacket(int seqnumber, int checksum, int data, byte[] dataValue) {
		super();
		this.seqnumber = seqnumber;
		this.checksum = checksum;
		this.data = data;
		this.dataValue = dataValue;
	}
	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}
	public int getData() {
		return data;
	}
	public void setData(int data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return "SendPacket [seqnumber=" + seqnumber + ", checksum=" + checksum + ", data=" + data + "]";
	}
	
	

}
