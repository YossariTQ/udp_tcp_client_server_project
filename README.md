# udp_tcp_client_server_project

A university coursework project.

TCP and UDP are two packet types used on an 802.x network to enable devices to communicate. This project aimed to create a simple client/server system using TCP and UDP packets to communicate.

Program requirements: 

  1. Performs a STOP and WAIT – with the limitation of a sliding window size of 1, as required.
     
  2. Program then increases the sliding window to a size of 5, and uses GO-BACK-N for retransmission, as required.
     
  3. The program transmits a sequence number (SN) as part of the data packet, as required.
  
  4. The receiving server examines the SN printing the data to the screen if it matches the expected SN, or payload. If it does not the ACKNOWLEDGEMENT is      sent back to the sender with a sliding window size of 5, as required.
