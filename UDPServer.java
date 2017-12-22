import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.swing.ButtonGroup;

public class UDPServer {

	static DatagramSocket serverSocket;
	static int port;
	static Integer[] windows;
	static int noOfPackets;
	static String protocol;
	static int baseNumber=0;
	static int expectedSeqNumberPosition=0;
	public static final double LOST_PACK_PROBABILITY = 0.1;
	static int windowSize;
	static int seqPosition;
	static int mss;
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		if(args.length==1)
		{
		
		port=Integer.parseInt(args[0]);
		byte[] incomingData = new byte[1024];
		try {
			serverSocket= new DatagramSocket(9002);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//while(true)
		//{
		try {

			DatagramPacket initialConnectionPacket = new DatagramPacket(incomingData, incomingData.length);
			serverSocket.receive(initialConnectionPacket);
			byte[] data1 = initialConnectionPacket.getData();
			ByteArrayInputStream initialBIS = new ByteArrayInputStream(data1);
			ObjectInputStream initialOIS = new ObjectInputStream(initialBIS);
			InitialConnectionData initialConnectionData = (InitialConnectionData) initialOIS.readObject();
			windows=initialConnectionData.getWindows();

			protocol=initialConnectionData.getProtocol();
			windowSize=initialConnectionData.getWindowSize();
			mss=initialConnectionData.getMss();
			noOfPackets=windows.length;

			for (int x : windows) {
				System.out.print(x +"|");

			}
			System.out.println("\n");

			if(protocol.equals("GBN"))
			{

				while(true)
				{
					if(expectedSeqNumberPosition<noOfPackets)
					{
						DatagramPacket dataPacket = new DatagramPacket(incomingData, incomingData.length);
						serverSocket.receive(dataPacket);
						int clientPort=dataPacket.getPort();
						byte[] recievedData = initialConnectionPacket.getData();
						ByteArrayInputStream dataGBNBAIS = new ByteArrayInputStream(recievedData);
						ObjectInputStream dataGBNOIS = new ObjectInputStream(dataGBNBAIS);
						SendPacket receivedGBNPacket = (SendPacket) dataGBNOIS.readObject();

						if(receivedGBNPacket.getSeqnumber()==windows[expectedSeqNumberPosition])
						{
							//need to replace with actual checksum calculation
							
							Checksum checksum = new CRC32();
							
					        checksum.update(receivedGBNPacket.getDataValue(), 0, receivedGBNPacket.getDataValue().length);
			
					        int computedChecksum = (int) checksum.getValue();
							//int computedChecksum=receivedGBNPacket.getDataValue().hashCode();
							//System.out.println("computed Checksum for packet "+expectedSeqNumberPosition+ "is "+ computedChecksum);
							//System.out.println("checksum in packket is "+ receivedGBNPacket.getChecksum());
							if(computedChecksum!=receivedGBNPacket.getChecksum())
							{
								System.out.println("Packet "+expectedSeqNumberPosition+ " seq no "+windows[expectedSeqNumberPosition] +  " recieved but its corrupted");
								//System.out.println("sending the ACK for correctly received packet");

								if(expectedSeqNumberPosition==0)
								{
									//do nothing for now , this is when the first packet is corrupted.	
								}
								else
								{
									/*AckPacket ackPacket= new AckPacket();
									ackPacket.setAckNumber(windows[expectedSeqNumberPosition-1]);
									DatagramPacket ackDataPacket= makeAckPacket(ackPacket,clientPort);
									serverSocket.send(ackDataPacket);
									System.out.println("Sending acknowledgment for correctly received packet "+(expectedSeqNumberPosition-1) + " with ack no "+windows[expectedSeqNumberPosition-1]);*/
								}
							}
							else
							{
								if(Math.random()<=LOST_PACK_PROBABILITY)
								{

									System.out.println("Packet loss occurred");
								}
								else
								{



									System.out.println("Packet "+ expectedSeqNumberPosition+ " seq no "+windows[expectedSeqNumberPosition] +  " recieved");
                                    System.out.println("Delivering the data :");
                                    String s= new String(receivedGBNPacket.getDataValue());
									System.out.println("data is:");
									System.out.println("--------------------------------------");
									System.out.println(s);
									System.out.println("--------------------------------------");
                                    
									AckPacket ackPacket= new AckPacket();
									ackPacket.setAckNumber(windows[expectedSeqNumberPosition]);
									DatagramPacket ackDataPacket= makeAckPacket(ackPacket,clientPort);
									serverSocket.send(ackDataPacket);
									System.out.println("Sending acknowledgment for packet "+expectedSeqNumberPosition + " with ack no "+windows[expectedSeqNumberPosition]);
									expectedSeqNumberPosition++;
								}
							}
						}
						else
						{
							System.out.println("Packet with seq number "+receivedGBNPacket.getSeqnumber()+ " discarded , expected Seq number is "+windows[expectedSeqNumberPosition]);
							
							if(expectedSeqNumberPosition==0)
							{
								// expecting the first packet and got some other packet so do nothing,so that time out will occur in sender side
								
							}
							else
							{
									AckPacket ackPacket= new AckPacket();
							ackPacket.setAckNumber(windows[expectedSeqNumberPosition-1]);
							DatagramPacket ackDataPacket= makeAckPacket(ackPacket,clientPort);
							serverSocket.send(ackDataPacket);
							System.out.println("Sending acknowledgment for correctly received packet "+(expectedSeqNumberPosition-1)+ " with ack no "+windows[expectedSeqNumberPosition-1]);
						}}

						//	System.out.println(receivedGBNPacket);
					}
					else
					{   
						
						DatagramPacket dataPacket = new DatagramPacket(incomingData, incomingData.length);
						serverSocket.receive(dataPacket);
						int clientPort=dataPacket.getPort();
						AckPacket ackPacket= new AckPacket();
						ackPacket.setAckNumber(windows[expectedSeqNumberPosition-1]);
						DatagramPacket ackDataPacket= makeAckPacket(ackPacket,clientPort);
						serverSocket.send(ackDataPacket);
						System.out.println("Sending acknowledgment for correctly received packet "+(expectedSeqNumberPosition-1)+ " with ack no "+windows[expectedSeqNumberPosition-1]);
						
					}
				}
				//System.out.println("Transaction done");

			}



			if(protocol.equals("SR"))
			{
				
				//ArrayList<SendPacket> dataList = new ArrayList<SendPacket>(noOfPackets);
				
				HashMap<Integer,SendPacket> dataList= new HashMap<Integer,SendPacket>();
				HashSet<Integer> unOrdered = new HashSet<>();
				expectedSeqNumberPosition=baseNumber+windowSize;
				//System.out.println("initail base number n seq n total no of pkst"+ baseNumber +""+ expectedSeqNumberPosition +" "+noOfPackets);


				while(true)
				{
					if(baseNumber<noOfPackets)
					{
						DatagramPacket dataPacket = new DatagramPacket(incomingData, incomingData.length);
						serverSocket.receive(dataPacket);
						int clientPort=dataPacket.getPort();
						byte[] recievedData = initialConnectionPacket.getData();
						ByteArrayInputStream dataGBNBAIS = new ByteArrayInputStream(recievedData);
						ObjectInputStream dataGBNOIS = new ObjectInputStream(dataGBNBAIS);
						SendPacket receivedSRPacket = (SendPacket) dataGBNOIS.readObject();


						for(int y=baseNumber;y<expectedSeqNumberPosition;y++)
						{
							if(y== noOfPackets)
								break;
							if(windows[y]==receivedSRPacket.getSeqnumber())
							{
								seqPosition=y;
							}
						}

						if(seqPosition>=baseNumber && seqPosition<expectedSeqNumberPosition)
						{
                            Checksum checksum = new CRC32();
							
					        checksum.update(receivedSRPacket.getDataValue(), 0, receivedSRPacket.getDataValue().length);
			
					        int computedChecksum = (int) checksum.getValue();
							//int computedChecksum=receivedGBNPacket.getDataValue().hashCode();
						//	System.out.println("computed Checksum for packet "+expectedSeqNumberPosition+ "is "+ computedChecksum);
						//	System.out.println("checksum in packket is "+ receivedSRPacket.getChecksum());
							if(computedChecksum!=receivedSRPacket.getChecksum())
							{
								System.out.println("\n");
								System.out.println("Received seq "+ receivedSRPacket.getSeqnumber() +" for packet "+ seqPosition+ "but its corrupted !!!");
								System.out.println("\n");
								
							
							}
							else
							{
							if(Math.random()<=LOST_PACK_PROBABILITY)
							{
								
								    System.out.println("\n");
									System.out.println("Packet loss occurred for packet "+seqPosition);
									System.out.println("\n");
							
								
							}
								
							else
							{
								
							System.out.println("Received seq "+ receivedSRPacket.getSeqnumber() +" for packet "+ seqPosition );
							//ack position = base number
							AckPacket ackPacket= new AckPacket();
							ackPacket.setAckNumber(windows[seqPosition]);
							DatagramPacket ackDataPacket= makeAckPacket(ackPacket,clientPort);
							serverSocket.send(ackDataPacket);
                            System.out.println("sending ack for packet "+seqPosition+ " with ack no "+windows[seqPosition]);
							unOrdered.add(seqPosition);
							dataList.put(seqPosition, receivedSRPacket);

							int fwdSlider=baseNumber;
							int tempExpectedSeqNumberPosition=expectedSeqNumberPosition;
							boolean bufferedFlag=true;
							while(fwdSlider < tempExpectedSeqNumberPosition)
							{

								if(unOrdered.contains(fwdSlider))
								{
									System.out.println("\n");
									System.out.println("Delivering packet"+ fwdSlider + " with seq number "+windows[fwdSlider]);
									
									SendPacket sendpkt= dataList.get(fwdSlider);
									String s= new String(sendpkt.getDataValue());
									System.out.println("data is:");
									System.out.println("--------------------------------------");
									System.out.println(s);
									System.out.println("--------------------------------------");
									expectedSeqNumberPosition++;
									fwdSlider++;
									bufferedFlag=false;
									
								}
								else
								{
									
									break;
								}

							}
							if(bufferedFlag)
							{
								System.out.println("Packet "+seqPosition+" buffered");
								System.out.println();
							}
							int slidingWindowSize= fwdSlider-baseNumber;
							baseNumber=baseNumber+slidingWindowSize;
						//	if(baseNumber+windowSize<noOfPackets)
						//	expectedSeqNumberPosition=expectedSeqNumberPosition+slidingWindowSize;
						//	System.out.println("basenumber " +baseNumber+"seq no"+expectedSeqNumberPosition);
							
							}
							
							}
						}
						else
						{
							if(unOrdered.contains(receivedSRPacket.getSeqnumber()))
									{
								System.out.println();
								System.out.println("Packet with seq number "+receivedSRPacket.getSeqnumber()+ " discarded !!!! and sending ack");
								AckPacket ackPacket= new AckPacket();
								ackPacket.setAckNumber(receivedSRPacket.getSeqnumber());
								DatagramPacket ackDataPacket= makeAckPacket(ackPacket,clientPort);
								serverSocket.send(ackDataPacket);
								System.out.println("Sending ack for "+receivedSRPacket.getSeqnumber());
								System.out.println();
									}
							else
							{
								System.out.println();
							System.out.println("Packet with seq number "+receivedSRPacket.getSeqnumber()+ " discarded !!!!");
							System.out.println();
							}
						}



					}
					else
					{
						
						/*if(baseNumber == noOfPackets)
						{*/
							
							//send the ack for final packet
						
							
							DatagramPacket dataPacket = new DatagramPacket(incomingData, incomingData.length);
							serverSocket.receive(dataPacket);
							int clientPort=dataPacket.getPort();
							byte[] recievedData = initialConnectionPacket.getData();
							ByteArrayInputStream dataGBNBAIS = new ByteArrayInputStream(recievedData);
							ObjectInputStream dataGBNOIS = new ObjectInputStream(dataGBNBAIS);
							SendPacket receivedSRPacket = (SendPacket) dataGBNOIS.readObject();
							if(unOrdered.contains(receivedSRPacket.getSeqnumber()))
							{
							AckPacket ackPacket= new AckPacket();
							ackPacket.setAckNumber(receivedSRPacket.getSeqnumber());
							DatagramPacket ackDataPacket= makeAckPacket(ackPacket,clientPort);
							serverSocket.send(ackDataPacket);
							System.out.println();
							System.out.println("Sending ack for "+receivedSRPacket.getSeqnumber());
							System.out.println();
							}
						/*}
						else
						{
						System.out.println("Transaction done");
						break;
						}*/
					}
					



				}

			}

			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//}






			//}

			//System.out.println("Hii");

	}
	else
	{
		System.out.println("Please give the arguments correctly");
	}
	}
		static DatagramPacket makeAckPacket(AckPacket ackPacket, int clientport)
		{

			DatagramPacket sendAckDatagramPacket = null;
			InetAddress IPAddress;
			try {
				IPAddress = InetAddress.getByName("localhost");


				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(ackPacket);
				os.close();
				byte[] data1 = outputStream.toByteArray();
				sendAckDatagramPacket = new DatagramPacket(data1, data1.length, IPAddress, clientport);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return sendAckDatagramPacket;
		}

	}
