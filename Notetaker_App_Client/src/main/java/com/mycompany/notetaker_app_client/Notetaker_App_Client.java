/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.notetaker_app_client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * name: Yakimah Wiley 
 * assignment: Project 2
 * date: 4/28/2025
 * class: CMPSC222 - Secure Coding
 *
 */
public class Notetaker_App_Client {

    /**
     * This class is specifically responsible for acting as the client between
     * the client-server interaction.
     * 
     * The user provides inputs based on the output recieved from the server
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        System.setProperty("javax.net.ssl.keyStore", "The location of the keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "Password");
        System.setProperty("javax.net.debug", "ssl");

        System.setProperty("javax.net.ssl.trustStore", "The location of the truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "Password"); 
        System.setProperty("javax.net.debug", "ssl");

        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (
                SSLSocket sslSocket = (SSLSocket) sslsocketfactory.createSocket("localhost", 9999); 
                BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
                InputStreamReader serverReader = new InputStreamReader(sslSocket.getInputStream());
                BufferedReader inFromServer = new BufferedReader(serverReader); 
                BufferedWriter outToServer = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));) {
            String line = null;
            String modifiedSentence;
            
            while (inFromServer != null ) {
                modifiedSentence = inFromServer.readLine();
                if(modifiedSentence.equals(" ")) break;
                else System.out.println("Line: " + modifiedSentence);
            }
            boolean exit = false;
            while (((line = inFromUser.readLine()) != null)) {                
                outToServer.write(line + '\n');
                outToServer.flush();
                System.out.println();
                while(inFromServer != null){
                    modifiedSentence = inFromServer.readLine();
                    if(modifiedSentence.equals(" ")) break;
                    else{
                        System.out.println("Line: " + modifiedSentence);
                        if (modifiedSentence.equals("Goodbye")) {
                            outToServer.write("Goodbye");
                            outToServer.flush();
                            exit = true;
                        }
                    }                    
                }
                if(exit) break;
                             
            }
            System.out.println("Thank you and goodbye");
            inFromUser.close();
            inFromServer.close();
            outToServer.close();
        }
    }
}
