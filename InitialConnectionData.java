import java.io.Serializable;

public class InitialConnectionData implements Serializable{
	
	int windowSize;
	String protocol;
	Integer[] windows;
	int mss;
	public int getMss() {
		return mss;
	}
	public void setMss(int mss) {
		this.mss = mss;
	}
	
	public InitialConnectionData(int windowSize, String protocol, Integer[] windows, int mss) {
		super();
		this.windowSize = windowSize;
		this.protocol = protocol;
		this.windows = windows;
		this.mss = mss;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public Integer[] getWindows() {
		return windows;
	}
	public void setWindows(Integer[] windows) {
		this.windows = windows;
	}
	

	
	public int getWindowSize() {
		return windowSize;
	}
	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}
	

}
