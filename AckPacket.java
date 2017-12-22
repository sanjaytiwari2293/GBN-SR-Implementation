import java.io.Serializable;

public class AckPacket  implements Serializable{
	
	int ackNumber;


	public int getAckNumber() {
		return ackNumber;
	}

	public void setAckNumber(int ackNumber) {
		this.ackNumber = ackNumber;
	}
	
	
}
