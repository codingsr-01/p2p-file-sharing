package NodoPeer;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class PeerClientServer implements Serializable{
    private String indirizzoIp;
    private int numPorta;
    private List<String> listaFile;
    private String percorso;
    private ServerSocket serverSocket;
    private boolean isRunning;
    protected final static int CERCAeDOWNLOAD=0,IDLE=1, TERMINA=2;

    public PeerClientServer(String indirizzoIp, int numPorta, List<String> listaFile, String percorso){
        this.indirizzoIp = indirizzoIp;
        this.numPorta = numPorta;
        this.percorso = percorso;
        this.listaFile = listaFile;
        this.isRunning = false;
    }//costruttore
    public static void main(String args[]) throws InterruptedException {
        //dati client
        System.out.println("Inserisci il numero di porta del client:");
        Scanner sc = new Scanner(System.in);
        int portaC = sc.nextInt();
        //
        System.out.println("Inserisci l'ID del client:");
        sc = new Scanner(System.in);
        int ID = sc.nextInt();
        String pathCartella = args[0] + ID;
        //
        //creazione cartella che distingue il client
        //
        List<String> lista = new ArrayList<>();
        File cartellaGenerale = new File(pathCartella);
        File[] files = cartellaGenerale.listFiles();
        if(!cartellaGenerale.exists()){
            System.out.println("Il client non dispone di file, creazione della sua cartella in corso...");
            cartellaGenerale.mkdir();
        }else{
            System.out.println("Cartella già esistente!");
        }
        if(!(files == null)){
            for(int i=0; i< files.length; ++i){
                if(!(files[i].getName().equals("download"))){
                    lista.add(files[i].getName());
                }
            }//for
        }//if

        String indirizzoClient;
        try {
            indirizzoClient = InetAddress.getLocalHost().getHostAddress();
            PeerClientServer client = new PeerClientServer(indirizzoClient, portaC, lista, pathCartella);
            avviaClient(client);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }//main
    public static void avviaClient(PeerClientServer client) throws InterruptedException {
        System.out.println("Inserisci l'indirizzo IP dell'IndexServer:");
        Scanner sc = new Scanner(System.in);
        String indirizzoServer = sc.nextLine();
        //
        System.out.println("Inserisci il numero di porta dell'IndexServer:");
        sc = new Scanner(System.in);
        int SERVER_PORT = sc.nextInt();

        Thread latoServer = new Thread(() -> client.initLatoServer());
        latoServer.start();
        List<PeerClientServer> peerConnessi = client.registrazioneAlServer(indirizzoServer,SERVER_PORT);
        peerConnessi.remove(client);//il numero di peer connessi è inteso a meno del peer che si registra
        boolean flagDownload = false, stop = false;
        List<Thread> downloadsInCorso= new ArrayList<>();
        Map<String, List<String>> listaFiles;
        List<PeerClientServer> peerPositivi;

        while(!stop){
            System.out.println("Inserisci operazione da effettuare: [0=CERCAeDOWNLOAD, 1=IDLE, 2=TERMINA]");
            sc = new Scanner(System.in);
            int numOp = sc.nextInt();

            String operazione;
            switch(numOp){
                case CERCAeDOWNLOAD -> {
                    operazione = "CERCA";
                    System.out.format("Operazione scelta: %s%n",operazione);

                    System.out.println("Aggiornamento della rete in corso...");
                    peerConnessi = client.aggiornaRete(indirizzoServer,SERVER_PORT);
                    System.out.println("Aggiornamento effettuato!");

                    System.out.println("La rete mette a disposizione i seguenti file!");
                    listaFiles = client.stampaLista(indirizzoServer,SERVER_PORT);
                    System.out.println(listaFiles);
                    System.out.println("Inserisci nome del file cercato:");
                    sc = new Scanner(System.in);
                    String nomeFile = sc.nextLine();
                    System.out.format("Nome inserito: %s%n",nomeFile);
                    peerPositivi = client.cercaFile(nomeFile, peerConnessi);
                    //System.out.println(peerPositivi);
                    PeerClientServer peerScelto = client.selezionaPeer(peerPositivi);
                    //System.out.println(peerScelto);
                    if(peerScelto != null){
                        //Meglio avviarlo con un thread distaccato, ci aiuta in fase di gestione delle terminazioni!
                        Thread threadDownload = new Thread(()->client.downloadFileDalPeer(peerScelto, nomeFile));
                        downloadsInCorso.add(threadDownload);
                        threadDownload.start();
                        flagDownload = true;
                    } else {
                        System.out.println("Nessun peer dispone del file cercato!");
                    }
                }
                case IDLE -> {
                    operazione = "IDLE";//rimane disponibile per gli altri peer
                    System.out.format("Operazione scelta: %s%n", operazione);
                    System.out.println("Client attivo solo lato server...");
                    System.out.println("Tra quanto tempo notificare le operazioni disponibili? [Millis]");
                    sc = new Scanner(System.in);
                    int tempoMillis = sc.nextInt();
                    Thread.sleep(tempoMillis);
                }
                case TERMINA -> {
                    operazione = "TERMINA";
                    System.out.format("Operazione scelta: %s%n", operazione);
                    client.shutdown();//si fa terminare la parte server così da non far accettare nessun tipo di richiesta!
                    if (flagDownload) {
                        System.out.println("Controllo di eventuali download in corso...");
                        for(Thread t : downloadsInCorso){
                            if(t.isAlive()){
                                System.out.format("Attendo il download in corso del thread %d%n", t.getId());
                                t.join();
                            }
                        }
                        System.out.println("Attesa terminata!");
                    }
                    client.disconnettiDalServer(indirizzoServer, SERVER_PORT);
                    stop = true;
                }
            }//switch
        }//while
    }
    private void initLatoServer(){
        try{
            serverSocket = new ServerSocket(numPorta);
            isRunning = true;
            System.out.println("Il lato server del client " + indirizzoIp + " è attivo sulla porta " + numPorta+ "!");
            while (isRunning){
                Socket accettaRichiesta = serverSocket.accept();
                System.out.println("Il client ha ricevuto una richiesta!");
                RichiestePeerHandler handlerPeer = new RichiestePeerHandler(accettaRichiesta, this);
                //
                handlerPeer.start();
            }
        } catch (IOException e){
            //e.printStackTrace();
        }
    }//init

    private void shutdown(){
        isRunning = false;
        try{
            serverSocket.close();
            System.out.println("Lato server chiuso, non posso accettare più richieste!");
            //
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    private Map<String, List<String>> stampaLista(String indirizzoIp, int numPorta){
        Map<String, List<String>> fileDisp = new HashMap<>();
        try {
            Socket socket = new Socket(indirizzoIp, numPorta);
            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos =  new ObjectOutputStream(os);
            //invio richiesta
            oos.writeObject("LISTA");
            oos.flush();
            //
            //ricezione della lista
            //
            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);

            fileDisp = (Map<String, List<String>>) ois.readObject();
            fileDisp.remove(this.indirizzoIp, this.listaFile);
            //

            oos.close();
            os.close();
            socket.close();
        } catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
        }

        return fileDisp;
    }
    //LATO CLIENT
    private List<PeerClientServer> registrazioneAlServer(String indirizzoIp, int numPorta){//posso inviare direttamente il server?
        List<PeerClientServer> peerConnessi = null;
        try{
            Socket socketReg = new Socket(indirizzoIp, numPorta);
            OutputStream os = socketReg.getOutputStream();
            ObjectOutputStream oos =  new ObjectOutputStream(os);

            //invio della richiesta al server
            PeerClientServer peerDaAggiungere = new PeerClientServer(this.indirizzoIp, this.numPorta, listaFile, percorso);
            oos.writeObject("REGISTRAZIONE");
            oos.writeObject(peerDaAggiungere);

            //ricezione della lista dei peer connessi alla rete
            InputStream is = socketReg.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);

            peerConnessi = (List<PeerClientServer>) ois.readObject();

            //Stampa la lista dei peer connessi alla rete

            oos.flush();
            is.close();
            socketReg.close();
            System.out.println("Registrazione effettuata!");

        }catch(IOException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        return peerConnessi;
    }

    private void disconnettiDalServer(String indirizzoIp, int numPorta){
        try {
            //
            Socket socketDis = new Socket(indirizzoIp, numPorta);
            OutputStream os = socketDis.getOutputStream();
            ObjectOutputStream oos =  new ObjectOutputStream(os);

            //invio richiesta disconnessione
            PeerClientServer peerDaDisconnettere = new PeerClientServer(this.indirizzoIp, this.numPorta, listaFile, percorso);
            oos.writeObject("DISCONNESSIONE");
            Thread.sleep(2500);
            oos.writeObject(peerDaDisconnettere);
            oos.flush();
            //System.out.println(peerDaDisconnettere);
            InputStream is = socketDis.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            //
            String ack = (String) ois.readObject();
            System.out.println(ack);
            //
            oos.close();
            os.close();
            ois.close();
            is.close();
            socketDis.close();
        } catch(IOException | InterruptedException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    private List<PeerClientServer> aggiornaRete(String indirizzoIp, int numPorta){
        List<PeerClientServer> peerConnessi = new ArrayList<PeerClientServer>();
        try {
            Socket socketRic = new Socket(indirizzoIp, numPorta);
            OutputStream os = socketRic.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);

            oos.writeObject("RICEZIONE");

            //ricezione della lista dei peer connessi alla rete
            InputStream is = socketRic.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);

            peerConnessi = (List<PeerClientServer>) ois.readObject();
            os.close();
            oos.close();
            is.close();
            ois.close();
        }catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
        return peerConnessi;
    }
    private List<PeerClientServer> cercaFile(String nomeFile, List<PeerClientServer> peerConnessi){
        peerConnessi.remove(this); //tolgo il client così non invia la richiesta a se stesso!
        List<PeerClientServer> peerPositivi = new ArrayList<PeerClientServer>();
        //invia la richiesta ad ogni peer connesso alla rete
        for(PeerClientServer peer: peerConnessi){
            try{
                Socket socket = new Socket(peer.getIndirizzoIp(), peer.getNumPorta());
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos =  new ObjectOutputStream(os);

                //invio della richiesta al peer (quest'ultimo agirà da server)
                //nel lato server ci deve essere la parte del peer che gestisce queste richieste
                oos.writeObject("CERCA");
                oos.writeObject(nomeFile);
                //
                InputStream is = socket.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(is);
                //lettura della risposta
                String risposta = (String) ois.readObject();
                //nodo della rete che contiene il file ricercato
                PeerClientServer peerOk;
                if (risposta.equals("TROVATO")){
                    peerOk = (PeerClientServer) ois.readObject();
                    peerPositivi.add(peerOk);
                }else if(risposta.equals("NIENTE")){
                    System.out.format("Il peer %s_%d non dispone del file %s%n", peer.getIndirizzoIp(), peer.getNumPorta(), nomeFile);
                }
                oos.close();
                os.close();
                is.close();
                ois.close();
                socket.close();
            } catch (IOException  e) {
                //e.printStackTrace();
                System.out.println("Connessione fallita!");
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }
        }
        return peerPositivi;
    }//cercaFile

    private PeerClientServer selezionaPeer(List<PeerClientServer> lista){ //(random se size > 1)
        PeerClientServer nodoSelezionato = null;
        Random random = new Random();
        int indice = 0;
        System.out.format("Il numero di peer che hanno a disposizone il File è %d!%n", lista.size());
        //scelta del peer da cui scaricare
        if (lista.size() > 1) {
            indice = random.nextInt(lista.size()); // uniforme su tutti i peer (prima: size()-1, l'ultimo non veniva mai scelto)
            nodoSelezionato = lista.get(indice);
        } else if (lista.size() == 1){
            nodoSelezionato = lista.get(indice);
        } else {
            System.out.println("Nessun peer da selezionare!");
        }
        return nodoSelezionato;
    }//selezionaPeer

    private void downloadFileDalPeer(PeerClientServer peer, String nomeFile) {
        String percorsoSalvataggio = percorso + "/" + "download";
        if(peer != null){//doppio controllo (lo faccio già in avviaClient())
            try{
                //creazione canale di comunicazione
                Socket socket = new Socket(peer.getIndirizzoIp(), peer.getNumPorta());
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                DataOutputStream dos = new DataOutputStream(os);//stream utilizzato per mantenere la codifica UTF

                //invio la richiesta di DOWNLOAD al peer scelto in fase di selezione
                oos.writeObject("DOWNLOAD");
                //System.out.format("Sono il client: %s%n", indirizzoIp + "_" + numPorta);
                dos.writeUTF(nomeFile);
                //
                //è in questo frangente che il lato server del peer agisce
                //
                //creazione cartella per il download
                boolean successo;
                File cartellaDownload = new File(percorsoSalvataggio);
                if(!cartellaDownload.exists()){
                    successo = cartellaDownload.mkdir();
                }else{
                    successo = true;
                    System.out.println("Cartella per il download già esistente!");
                }

                //ricezione del file e copia nel file system locale (client che lo ha richiesto)
                if(successo) {
                    System.out.println("Sto aspettando il download del file...");
                    InputStream is = socket.getInputStream();
                    FileOutputStream fos = new FileOutputStream(percorsoSalvataggio + "/" + nomeFile,true);

                    byte[] buffer = new byte[4096];
                    int byteLetti;
                    while ((byteLetti = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, byteLetti);
                    }
                    fos.close();
                    is.close();
                }//if_successo

                //chiusura stream e connessione
                oos.close();
                os.close();
                dos.close();
                socket.close();
                System.out.format("Il file %s è stato scaricato con successo!%n", nomeFile);
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public String getPercorso() {
        return percorso;
    }

    public List<String> getListaFile() {
        return listaFile;
    }

    public int getNumPorta() {
        return numPorta;
    }

    public String getIndirizzoIp() {
        return indirizzoIp;
    }

    @Override
    public String toString() {
        return "PeerClientServer{" +
                "indirizzoIp='" + indirizzoIp + '\'' +
                ", numPorta=" + numPorta +
                ", listaFile=" + listaFile +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerClientServer that = (PeerClientServer) o;
        return numPorta == that.numPorta && indirizzoIp.equals(that.indirizzoIp) && listaFile.equals(that.listaFile) && percorso.equals(that.percorso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indirizzoIp, numPorta, listaFile, percorso);
    }
}
