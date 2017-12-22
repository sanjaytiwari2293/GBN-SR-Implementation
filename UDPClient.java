import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.sound.sampled.DataLine;

public class UDPClient {


	static String protocol;
	static int seqBits;
	static int windowsize;
	static int timeout;
	static int mss;
	static Integer[] windows;
	static int lastSeqNumber;
	static int noofpkts;
	static int port;
	static int baseNumber=0;
	static int nextSeqNumberPosition=0;
	static int ackPosition;
    static ArrayList<byte[]> dataList = new ArrayList<byte[]>();
    static String dataFilePath;
	

	public static int TIMER = 3000;

	public static final double LOST_ACK_PROBABILITY = 0.05;

	public static final double BIT_ERROR_PROBABILITY = 0.5;



	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		DatagramSocket clientSocket=new DatagramSocket();
		byte[] receiveAckData = new byte[1024];
		if(args.length==4)
		{

			String filePath=args[0];
			port=Integer.parseInt(args[1]);
			noofpkts=Integer.parseInt(args[2]);
			dataFilePath=args[3];
			try {
				readingInputFile(filePath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Protocol: "+protocol+" Total no of packets "+noofpkts+" seqBits: "+seqBits+" windowSize "+windowsize+" timeout "+ timeout+ " mss "+mss);
			System.out.println();
			String myfile = dataFilePath;
	        File file = new File(myfile);
	       
	        byte[] arr = new byte[mss];
	    

	        ByteArrayOutputStream ous = null;
	        InputStream ios = null;
	        try {
	            byte[] buffer = new byte[25];
	            ous = new ByteArrayOutputStream();
	            ios = new FileInputStream(file);
	            int read = 0;
	            while ((read = ios.read(buffer)) != -1) {
	                ous.write(buffer, 0, read);
	                arr = ous.toByteArray();
	                dataList.add(arr);
	                ous.reset();

	            }
	            //System.out.println("x "+arrayList.size());

	        }catch(Exception e){
	            e.printStackTrace();
	        }
	        
	        System.out.println("Total lenght of data list " +dataList.size());
			
			prepareWindow(seqBits,windowsize);

			for (int x : windows) {
				System.out.print(x +"|");

			}
			System.out.println("");

			if(protocol.equals("GBN"))
			{

				gbnHandler(clientSocket);
			}
			else
			{
				srHandler(clientSocket);
			}






		}
		//clientSocket.close();
	}


	static void gbnHandler(DatagramSocket clientSocket) throws IOException, ClassNotFoundException
	{


	
		//sending initial data
		try {

			InitialConnectionData initialData= new InitialConnectionData(windowsize,"GBN",windows,mss);
			InetAddress IPAddress;
			IPAddress = InetAddress.getByName("localhost");
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(initialData);
			os.close();
			byte[] data1 = outputStream.toByteArray();
			DatagramPacket initialPacket = new DatagramPacket(data1, data1.length, IPAddress, port);
			System.out.println("Sending Initial Connection Data !!!!!!!!!!!" + "\n");
			clientSocket.send(initialPacket);
			Thread.sleep(TIMER);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sending first full window 


		int x=0;
		while(x<windowsize)
		{
		//	int checkSum= dataList.get(x).hashCode();
			
			
			Checksum checksum = new CRC32();
			checksum.update(dataList.get(x), 0, dataList.get(x).length);
			        int checkSum = (int) checksum.getValue();
			       // System.out.println("Checksum is for packet "+x+" "+checkSum);

			SendPacket sendpacket= new SendPacket(windows[x],checkSum,0,dataList.get(x));
			DatagramPacket sendDatagramPacket=makePacket(sendpacket);
			
			
			

			System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with sequence number "+windows[x]+"\n");
			String s = new String(dataList.get(x));
			System.out.println("Data is : ");
			System.out.println("------------------------------------------------");
			System.out.println(s);
			System.out.println("------------------------------------------------");
			System.out.println();
			try {
				clientSocket.send(sendDatagramPacket);
				x++;
				nextSeqNumberPosition++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
		if(x==windowsize)
			System.out.println("Timer started for packet "+ baseNumber + " with seq number "+ windows[baseNumber]);
		    System.out.println();
		byte[] receiveData = new byte[1024];
		clientSocket.setSoTimeout(timeout);

		while(true)
		{
			//System.out.println("Hello");



			try {

				DatagramPacket rPkt = new DatagramPacket(receiveData, receiveData.length);
			//	clientSocket.setSoTimeout(3000);
				clientSocket.receive(rPkt);
				byte[] recievedData = rPkt.getData();
				ByteArrayInputStream ackDataGBNBAIS = new ByteArrayInputStream(recievedData);
				ObjectInputStream dataGBNOIS;
				dataGBNOIS = new ObjectInputStream(ackDataGBNBAIS);
				AckPacket receivedAckGBNPacket = (AckPacket) dataGBNOIS.readObject();





				for(int y=baseNumber;y<nextSeqNumberPosition;y++)
				{
					if(baseNumber < noofpkts)
					{
					if(windows[y]==receivedAckGBNPacket.getAckNumber())
					{
						ackPosition=y;
					}
					}
				}

		if(ackPosition ==noofpkts-1)
				{
					System.out.println("Received ack "+ receivedAckGBNPacket.getAckNumber()+ " for packet "+ ackPosition );
				}
				/*if(baseNumber==nextSeqNumberPosition-1)
				{
					System.out.println("Timer stopped , All acknowledgement received successfully");
					clientSocket.close();
					break;
				}*/
		        if(ackPosition==noofpkts-1)
		        {
		        	
		        	System.out.println("All packets are received");
					//System.out.println("Received ack "+ receivedAckGBNPacket.getAckNumber()+ " for packet "+ ackPosition );
					System.out.println("Timer stopped , All acknowledgement received successfully");
					clientSocket.close();
					break;
		        }
		        else if(ackPosition>=baseNumber && ackPosition<nextSeqNumberPosition)
				{


					if(Math.random()<=LOST_ACK_PROBABILITY)
					{

						System.out.println("Acknowledgement lost !!!!!! "+ receivedAckGBNPacket.getAckNumber()+ " for packet "+ ackPosition);
					    System.out.println();
					}
					else
					{
						System.out.println("Received ack "+ receivedAckGBNPacket.getAckNumber()+ " for packet "+ ackPosition );
						
						int slidingWindowSize= ackPosition-baseNumber+1;
						baseNumber=ackPosition+1;

						System.out.println("Timer started for packet "+ baseNumber + " with seq number "+ windows[baseNumber]);

						if(nextSeqNumberPosition<noofpkts)
						{

							for(int z=0;z<slidingWindowSize;z++)
							{

								SendPacket sendpacket;
								if(Math.random()<=BIT_ERROR_PROBABILITY)
								{
									
									//calculating the  checksum
									Checksum checksum = new CRC32();
							        checksum.update(dataList.get(nextSeqNumberPosition), 0, dataList.get(nextSeqNumberPosition).length);
							        int checkSum = (int) checksum.getValue();
							     //   System.out.println("Checksum is for packet "+nextSeqNumberPosition+" "+checkSum);
									//corrupt the content 
									byte[] corruptedData= "corruptedData".getBytes();
									sendpacket= new SendPacket(windows[nextSeqNumberPosition],checkSum,0,corruptedData);  
									System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with ERROR  sequence number "+windows[nextSeqNumberPosition]+"\n");
									String s = new String(dataList.get(nextSeqNumberPosition));
									System.out.println("Data is : ");
									System.out.println("------------------------------------------------");
									System.out.println(s);
									System.out.println("------------------------------------------------");
									System.out.println();
									//clientSocket.setSoTimeout(3000);
								
								}
								else
								{	
									Checksum checksum = new CRC32();
							        checksum.update(dataList.get(nextSeqNumberPosition), 0, dataList.get(nextSeqNumberPosition).length);
					
							        int checkSum = (int) checksum.getValue();
							      //  System.out.println("Checksum is for packet "+nextSeqNumberPosition+" "+checkSum);
									sendpacket= new SendPacket(windows[nextSeqNumberPosition],checkSum,0,dataList.get(nextSeqNumberPosition));
									System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with sequence number "+windows[nextSeqNumberPosition]+"\n");
									String s = new String(dataList.get(nextSeqNumberPosition));
									System.out.println("Data is : ");
									System.out.println("------------------------------------------------");
									System.out.println(s);
									System.out.println("------------------------------------------------");
									System.out.println();
									//clientSocket.setSoTimeout(3000);
								}
								DatagramPacket sendDatagramPacket=makePacket(sendpacket);
								//System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with sequence number "+windows[nextSeqNumberPosition]+"\n");
								clientSocket.send(sendDatagramPacket);
								//if(nextSeqNumberPosition<noofpkts-1)
								nextSeqNumberPosition++ ;
								//Thread.sleep(2000);

							}


						}



					}
				}
				else
				{
					System.out.println("Discarding the acknowlegement packet "+ackPosition);
					
				}

			} catch (SocketTimeoutException e) {
				// TODO Auto-generated catch block
				System.out.println("Timeout occurred for Packet " + baseNumber + " with seq number " +windows[baseNumber]);
				System.out.println("Resending packets !!!!!!!!!!");

				for(int i=baseNumber;i<nextSeqNumberPosition;i++)
				{
                    
					Checksum checksum = new CRC32();
			        checksum.update(dataList.get(i), 0, dataList.get(i).length);
	
			        int checkSum = (int) checksum.getValue();
			      //  System.out.println("Checksum is for packet "+i+" "+checkSum);
					SendPacket sendpacket= new SendPacket(windows[i],checkSum,0,dataList.get(i));
					DatagramPacket sendDatagramPacket=makePacket(sendpacket);
					System.out.println("Sending Packet Number "+i+ " with sequence number "+windows[i]);
					String s = new String(dataList.get(i));
					System.out.println("Data is : ");
					System.out.println("------------------------------------------------");
					System.out.println(s);
					System.out.println("------------------------------------------------");
					System.out.println();
					clientSocket.send(sendDatagramPacket);
					


				}
				
			}





		}




	}

	static void srHandler(DatagramSocket clientSocket) throws IOException, ClassNotFoundException
	{

		HashSet<Integer> unOrdered = new HashSet<>();


		/*try {

		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		//sending initial data
		try {

			InitialConnectionData initialData= new InitialConnectionData(windowsize,"SR",windows,mss);
			InetAddress IPAddress;
			IPAddress = InetAddress.getByName("localhost");
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(initialData);
			os.close();
			byte[] data1 = outputStream.toByteArray();
			DatagramPacket initialPacket = new DatagramPacket(data1, data1.length, IPAddress, port);
			System.out.println("Sending Initial Connection Data !!!!" + "\n");
			System.out.println();
			clientSocket.send(initialPacket);
			Thread.sleep(TIMER);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sending first full window 


		int x=0;
		while(x<windowsize)
		{

			//SendPacket sendpacket= new SendPacket(windows[x],0,0);
			Checksum checksum = new CRC32();
	        checksum.update(dataList.get(x), 0, dataList.get(x).length);

	        int checkSum = (int) checksum.getValue();
	      //  System.out.println("Checksum is for packet "+x+" "+checkSum);
			SendPacket sendpacket= new SendPacket(windows[x],checkSum,0,dataList.get(x));
			DatagramPacket sendDatagramPacket=makePacket(sendpacket);
            
			System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with sequence number "+windows[x]+"\n");
			String s = new String(dataList.get(x));
			System.out.println("Data is : ");
			System.out.println("------------------------------------------------");
			System.out.println(s);
			System.out.println("------------------------------------------------");
			try {
				clientSocket.send(sendDatagramPacket);
				x++;
				nextSeqNumberPosition++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
		if(x==windowsize)
		{
			System.out.println();
			System.out.println("Timer started for packet "+ baseNumber + " with seq number "+ windows[baseNumber]);
		    System.out.println();
		}
			byte[] receiveData = new byte[1024];
			clientSocket.setSoTimeout(timeout);
		while(true)
		{
			
			if(baseNumber<noofpkts)
			{
			try {

				DatagramPacket rPkt = new DatagramPacket(receiveData, receiveData.length);
				//clientSocket.setSoTimeout(3000);
				clientSocket.receive(rPkt);
				byte[] recievedData = rPkt.getData();
				ByteArrayInputStream ackDataGBNBAIS = new ByteArrayInputStream(recievedData);
				ObjectInputStream dataGBNOIS;
				dataGBNOIS = new ObjectInputStream(ackDataGBNBAIS);
				AckPacket receivedAckGBNPacket = (AckPacket) dataGBNOIS.readObject();


				
                boolean isPresentInWindow=false;
				for(int y=baseNumber;y<nextSeqNumberPosition;y++)
				{
					if(y== noofpkts)
						break;
					if(windows[y]==receivedAckGBNPacket.getAckNumber())
					{
						ackPosition=y;
						isPresentInWindow=true;
						//System.out.println(ackPosition+","+baseNumber+","+nextSeqNumberPosition);
					}
					
				}



				

				if(isPresentInWindow &&ackPosition>=baseNumber && ackPosition<nextSeqNumberPosition)
				{
					if(Math.random()<=LOST_ACK_PROBABILITY)
					{
             
						System.out.println("Acknowledgement lost !!!!!! "+ receivedAckGBNPacket.getAckNumber()+ " for packet "+ ackPosition);
					    System.out.println();
					}
					else
					{
					
					
					
					System.out.println("Received ack "+ receivedAckGBNPacket.getAckNumber()+ " for packet "+ ackPosition );
					System.out.println();
					//ack position = base number
					
					
					unOrdered.add(ackPosition);
					
					int fwdSlider=baseNumber;
					while(fwdSlider < nextSeqNumberPosition)
					{
						
						if(unOrdered.contains(fwdSlider))
						{
							fwdSlider++;
						}
						else
						{
							break;
						}
						
					}
					 int slidingWindowSize= fwdSlider-baseNumber;
					 baseNumber=baseNumber+slidingWindowSize;
                     /*if(slidingWindowSize>0 && baseNumber != noofpkts)
					 System.out.println("Timer started for packet "+baseNumber+" with seq no "+windows[baseNumber] );*/
						
                     if(nextSeqNumberPosition<noofpkts)
						{

							for(int z=0;z<slidingWindowSize;z++)
							{

								SendPacket sendpacket;
								if(Math.random()<=BIT_ERROR_PROBABILITY)
								{
									//calculating the checksum
									Checksum checksum = new CRC32();
							        checksum.update(dataList.get(nextSeqNumberPosition), 0, dataList.get(nextSeqNumberPosition).length);
					
							        int checkSum = (int) checksum.getValue();
							       // System.out.println("Checksum is for packet "+nextSeqNumberPosition+" "+checkSum);
									
									//corrupt the content 
							        byte[] corruptedData= "corruptedData".getBytes();
							        
									if(nextSeqNumberPosition<noofpkts)
									{
									sendpacket= new SendPacket(windows[nextSeqNumberPosition],checkSum,0,corruptedData);  
									System.out.println();
									System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with ERROR  sequence number "+windows[nextSeqNumberPosition]+"\n");
									String s = new String(dataList.get(nextSeqNumberPosition));
									System.out.println("Data is : ");
									System.out.println("---------------------------------------------------");
									System.out.println(s);
									System.out.println("---------------------------------------------------");
									DatagramPacket sendDatagramPacket=makePacket(sendpacket);
									//System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with sequence number "+windows[nextSeqNumberPosition]+"\n");
									clientSocket.send(sendDatagramPacket);
									nextSeqNumberPosition++ ;
									}
									}
								else
								{	
									if(nextSeqNumberPosition<noofpkts)
									{
										Checksum checksum = new CRC32();
								        checksum.update(dataList.get(nextSeqNumberPosition), 0, dataList.get(nextSeqNumberPosition).length);
						
								        int checkSum = (int) checksum.getValue();
							//	        System.out.println("Checksum is for packet "+nextSeqNumberPosition+" "+checkSum);
									System.out.println();
									sendpacket= new SendPacket(windows[nextSeqNumberPosition],checkSum,0,dataList.get(nextSeqNumberPosition));
									System.out.println();
									System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with sequence number "+windows[nextSeqNumberPosition]+"\n");
									String s = new String(dataList.get(nextSeqNumberPosition));
									System.out.println("Data is : ");
									System.out.println("---------------------------------------------------");
									System.out.println(s);
									System.out.println("---------------------------------------------------");
									DatagramPacket sendDatagramPacket=makePacket(sendpacket);
									//System.out.println("Sending Packet Number "+nextSeqNumberPosition+ " with sequence number "+windows[nextSeqNumberPosition]+"\n");
									clientSocket.send(sendDatagramPacket);
									nextSeqNumberPosition++ ;
									}
								}
								
								//Thread.sleep(2000);

							}
							if(slidingWindowSize>0 && baseNumber != noofpkts)
								System.out.println();
							     if(baseNumber<ackPosition)
							    	 System.out.println("Timer is already running for packet "+ baseNumber+ " with seq no "+windows[baseNumber]);
							     else
							     System.out.println("Timer started for packet "+baseNumber+" with seq no "+windows[baseNumber] );
                                System.out.println();
						}

				}

					}
				
				else
				{
					System.out.println();
					System.out.println("Discarding the ack with ack number " +receivedAckGBNPacket.getAckNumber() +"!!!!");
					System.out.println();
				}

			} catch (SocketTimeoutException e) {
				// TODO Auto-generated catch block
				System.out.println();
				System.out.println("Timeout occurred for Packet " + baseNumber + " with seq number " +windows[baseNumber]);
				System.out.println("Resending packet !!!!!!!!!!");
				Checksum checksum = new CRC32();
		        checksum.update(dataList.get(baseNumber), 0, dataList.get(baseNumber).length);

		        int checkSum = (int) checksum.getValue();
		   //     System.out.println("Checksum is for packet "+baseNumber+" "+checkSum);

					SendPacket sendpacket= new SendPacket(windows[baseNumber],checkSum,0,dataList.get(baseNumber));
					
					DatagramPacket sendDatagramPacket=makePacket(sendpacket);
					
					System.out.println("Sending Packet Number "+baseNumber+ " with sequence number "+windows[baseNumber]);
					
					String s = new String(dataList.get(baseNumber));
					System.out.println("Data is : ");
					System.out.println("------------------------------------------------");
					System.out.println(s);
					System.out.println("------------------------------------------------");
					System.out.println();
					clientSocket.send(sendDatagramPacket);


				
			}
			}
			else
			{
				System.out.println();
				System.out.println("All acknowledgments are received");
				break;
				
			}
		}
		}
	




	static void readingInputFile(String filepath) throws IOException
	{

		BufferedReader br= new BufferedReader(new FileReader(filepath));
		String line="";
		int lineNumber=0;
		while((line=br.readLine())!=null)
		{

			if(lineNumber==0)
			{
				protocol=line;
			}else if(lineNumber==1)
			{
				String[] arrays=line.split(" ");
				seqBits=Integer.parseInt(arrays[0]);
				windowsize=Integer.parseInt(arrays[1]);

			}else if(lineNumber==2)
			{
				timeout=Integer.parseInt(line);


			}else
			{
				mss=Integer.parseInt(line);
			}

			lineNumber++;
		}
		br.close();

	}

	static void prepareWindow(int seqbits, int windowsize)
	{

		int seqnum=(int) Math.pow(2,seqbits);
		lastSeqNumber=seqnum-1;
		windows= new Integer[noofpkts];
		int j=0;
		for( int i=0;i<noofpkts;i++)
		{
			if(j==seqnum)
			{
				j=0;
			}
			windows[i]=j;
			j++;

		}

	}
	static DatagramPacket makePacket(SendPacket sendpacket)
	{

		DatagramPacket sendDatagramPacket = null;
		InetAddress IPAddress;
		try {
			IPAddress = InetAddress.getByName("localhost");

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(sendpacket);
			os.close();
			byte[] data1 = outputStream.toByteArray();
			sendDatagramPacket = new DatagramPacket(data1, data1.length, IPAddress, port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sendDatagramPacket;
	}

	
}
