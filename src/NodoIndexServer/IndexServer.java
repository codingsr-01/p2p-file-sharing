package NodoIndexServer;

import NodoPeer.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class IndexServer {
    private int numPortaServer;
    private List<PeerClientServer> peerConnessi;
    private boolean isRunning;
    private ServerSocket serverSocket;

    public IndexServer(int numPortaServer){
        this.numPortaServer = numPortaServer;
        this.peerConnessi= new ArrayList<PeerClientServer>();
        this.isRunning = false;
    }
    public static void main(String args[]) throws UnknownHostException {
        String indirizzoServer = InetAddress.getLocalHost().getHostAddress();
        System.out.format("Il server ha il seguente indirizzo IP: %s%n",indirizzoServer);
        int SERVER_PORT = 8085;
        IndexServer indexServer = new IndexServer(SERVER_PORT);
        Thread threadServer = new Thread(()-> indexServer.inizia());
        threadServer.start();

        System.out.println("Digitare TERMINA per interrompere l'esecuzione del server!");
        Scanner sc = new Scanner(System.in);
        String op = sc.nextLine();
        if(op.equals("TERMINA")){
            indexServer.shutdown();
        }
    }//main
    public void inizia(){
        try{
            serverSocket = new ServerSocket(numPortaServer);
            isRunning = true;
            System.out.println("Il server è attivo sulla porta " + numPortaServer + "!");
            while (isRunning){
                Socket accettaRichiesta = serverSocket.accept();
                RichiesteServerHandler handlerServer = new RichiesteServerHandler(accettaRichiesta, this);
                handlerServer.start();
            }
        } catch (IOException e){
            //e.printStackTrace();
        }
    }//init
    public void shutdown(){
        isRunning = false;
        try{
            serverSocket.close();
            System.out.println("Server chiuso!");
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    //connetti un peer alla rete
    public void connettiPeer(PeerClientServer peer){
        peerConnessi.add(peer);
    }
    //disconetti un peer dalla rete
    public void disconnettiPeer(PeerClientServer peer){
        peerConnessi.remove(peer);
    }
    public List<PeerClientServer> getPeerConnessi() {
        return peerConnessi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexServer)) return false;
        IndexServer that = (IndexServer) o;
        return numPortaServer == that.numPortaServer && serverSocket.equals(that.serverSocket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numPortaServer, serverSocket);
    }
}//classbody
