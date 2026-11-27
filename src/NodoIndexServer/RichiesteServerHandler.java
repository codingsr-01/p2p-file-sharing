package NodoIndexServer;

import NodoPeer.PeerClientServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RichiesteServerHandler extends Thread{
    private Socket socketPeer;
    private IndexServer indexServer;

    public RichiesteServerHandler(Socket socketPeer, IndexServer indexServer){
        this.socketPeer = socketPeer;
        this.indexServer = indexServer;
    }
    public void run(){
        synchronized (indexServer.getPeerConnessi()){
            try{
                InputStream is = socketPeer.getInputStream();
                OutputStream os = socketPeer.getOutputStream();
                ObjectInputStream ois = new ObjectInputStream(is);
                ObjectOutputStream oos = new ObjectOutputStream(os);

                //lettura tipo di richiesta
                String richiesta = (String) ois.readObject();

                //gestione della richiesta
                if (richiesta.equals("REGISTRAZIONE")) {
                    PeerClientServer peer = (PeerClientServer) ois.readObject();
                    indexServer.connettiPeer(peer);
                    //invio della lista di peer connessi
                    oos.writeObject(indexServer.getPeerConnessi());
                    oos.flush();
                    System.out.println("Si è connesso il seguente peer:");
                    System.out.println(peer);
                } else if (richiesta.equals("RICEZIONE")) {
                    oos.writeObject(indexServer.getPeerConnessi());
                    oos.flush();
                } else if(richiesta.equals("DISCONNESSIONE")){
                    PeerClientServer peer = (PeerClientServer) ois.readObject();
                    indexServer.disconnettiPeer(peer);
                    String ack = "Richiesta disconnesione ricevuta!";
                    oos.writeObject(ack);
                    oos.flush();
                    System.out.println("Di seguito la lista dei peer attalmente connessi alla rete:");
                    System.out.println(indexServer.getPeerConnessi());
                } else if(richiesta.equals("LISTA")){
                    Map<String, List<String>> listaFileP2P = new HashMap<>();
                    for(PeerClientServer peer: indexServer.getPeerConnessi()){
                        listaFileP2P.put(peer.getIndirizzoIp(),peer.getListaFile());
                    }
                    oos.writeObject(listaFileP2P);
                    oos.flush();
                }

                os.close();
                oos.close();
                is.close();
                ois.close();
                socketPeer.close();
            } catch(IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }//run
}//
