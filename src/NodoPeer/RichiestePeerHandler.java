package NodoPeer;

import java.io.*;
import java.net.Socket;
import java.util.Random;

public class RichiestePeerHandler extends Thread{
    private Socket socketPeer;
    private PeerClientServer peerCS;
    public RichiestePeerHandler(Socket socketPeer, PeerClientServer peerCS){
        this.socketPeer = socketPeer;
        this.peerCS = peerCS;
    }
    public void run(){
        synchronized (peerCS.getListaFile()){
            Thread gestioneRchiesta = new Thread(() -> {
                try{
                    InputStream is = socketPeer.getInputStream();
                    OutputStream os = socketPeer.getOutputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    // L'ObjectOutputStream scrive subito il suo header (0xACED0005) sul socket:
                    // va creato solo per la risposta a CERCA, altrimenti finisce in testa al file inviato col DOWNLOAD.
                    ObjectOutputStream oos = null;

                    //lettura tipo di richiesta
                    String richiesta = (String) ois.readObject();
                    System.out.format("Richiesta ricevuta: %s%n", richiesta);

                    //gestione della richiesta
                    if (richiesta.equals("CERCA")){
                        oos = new ObjectOutputStream(os);
                        String nomeFileCercato = (String) ois.readObject();
                        boolean filePresente = peerCS.getListaFile().contains(nomeFileCercato);
                        if(filePresente){
                            oos.writeObject("TROVATO");
                            PeerClientServer peerSi = new PeerClientServer(peerCS.getIndirizzoIp(), peerCS.getNumPorta(), peerCS.getListaFile(), peerCS.getPercorso());
                            oos.writeObject(peerSi);
                            oos.flush();
                            System.out.format("Dispongo del file richiesto: %s%n",nomeFileCercato);
                        }else{
                            oos.writeObject("NIENTE");
                            oos.flush();
                            System.out.format("Non dispongo del file richiesto: %s%n",nomeFileCercato);
                        }
                    } else if(richiesta.equals("DOWNLOAD")){
                        System.out.println("Sono il client scelto per il download...");
                        //se al client arriva una richiesta di DOWNLOAD vuol dire è stato scelto come peer da cui scaricare
                        DataInputStream input = new DataInputStream(socketPeer.getInputStream());//in questo modo i file inviati mantengono la codifica UTF
                        String nomeFileCercato = input.readUTF();
                        System.out.format("Il file da inviare è %s:%n", nomeFileCercato);

                        //Simulazione tempo di download
                        System.out.println("Invio in corso...");
                        Random random = new Random();
                        int massimo = 40000;
                        int minimo = 10000;
                        int tempo = random.nextInt(massimo) + minimo;
                        Thread.sleep(tempo);
                        //cerca all'interno della cartella dove ci stanno i files del client, il file "nomeFileCercato"
                        String path = peerCS.getPercorso() + "/" + nomeFileCercato;
                        FileInputStream fis = new FileInputStream(path);
                        byte[] buf = new byte[4096];
                        int read;
                        while ((read = fis.read(buf)) != -1) {
                            os.write(buf, 0, read);
                        }
                        fis.close();
                        input.close();
                        System.out.format("Il file %s è stato inviato!%n", nomeFileCercato);
                    }//if
                    if (oos != null) oos.close();
                    os.close();
                    ois.close();
                    is.close();
                    socketPeer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    //System.out.println("Il client che si sta cercando di contattare non è online!");//ha chiuso il lato server!
                } catch(ClassNotFoundException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
            gestioneRchiesta.start();
        }//synchronized
    }//run
}//classbody
