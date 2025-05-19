/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.notetaker_app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 *
 * name: Yakimah Wiley 
 * assignment: Project 2
 * date: 4/28/2025
 * class: CMPSC222 - Secure Coding
 *
 */
public class Notetaker_App {

    public static void main(String[] args){
        SetProperties();
        
        String filename = "The file responsible for calling jdbc";
        SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try (
                SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(9999);
                SSLSocket sslSocket = (SSLSocket) sslserversocket.accept(); 
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
                ){
            DB_Connect db = new DB_Connect(filename, outToClient);

            String line = null;
            while (((line = inFromClient.readLine()) != null)) {
               System.out.println("Received from client:" + line);
               db.checkInput(line);
            }
            inFromClient.close();
            inFromClient.close();
            System.exit(0);
        }catch(IOException ex){
            System.out.println(ex.getMessage());
        }
    }
    
    private static void SetProperties(){
        System.setProperty("javax.net.ssl.keyStore", "The location of the keystore");
        System.setProperty("javax.net.ssl.keyStorePassword", "Password"); 
        System.setProperty("javax.net.debug", "ssl");

        System.setProperty("javax.net.ssl.trustStore", "The location of the truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "Password"); 
        System.setProperty("javax.net.debug", "ssl");
    }

}
