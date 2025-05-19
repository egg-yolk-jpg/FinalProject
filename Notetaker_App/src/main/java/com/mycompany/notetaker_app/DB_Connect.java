/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.notetaker_app;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Apache_PHP
 */
public class DB_Connect {
    //Variable declarations that are responsible for holding lists of dialog within the application
    private static final String[] states = {"logged in","logging in", "creating account", "signed out", "on menu"};
    private static final String[] logging_in_states = {"obtaining username", "obtaining password", "checking database"};
    private static final String[] creating_account_states = {"validating username", "setting password", "uploading to database"};
    private static final String[] message_states = {"main menu", "reading messages", "composing message", "uploading to database", "obtaining user input", "selecting recipient"};
    private static final String[] informative = {"get user", "get pass", "user constraints", "pass constraints", "message created", "db add error", "upload success", "message upload error", "message constraints"};
    private static final ArrayList<String> recipientList = new ArrayList<>();
    private static final ArrayList<String> recievedMessages = new ArrayList<>();
    private static Map<Integer, String> err;
    private static Map<Integer, String> success;

    
    /**
     * successful user validation : state = logging in; validate password
     * unsuccessful user validation: state = signed out; return to main menu
     * 
     * successful account creation: state = logged in; secondary menu options
     * account creation - username already exists: send error message, request new username until a sufficient one is found
     */


    private static String db_file;
    private static String current_state = states[3];
    private static String secondary_state;
    private static int tertiary_state;
    private static BufferedWriter outToClient;

    
    
    private static String username;
    private static String pass_hash;
    private static int menu_selection;
    private static String recipient;
    private static boolean recipient_selection;
    private static String user_message;
    
    private static void StatesReset(){
        current_state = states[4];
        secondary_state = null;
        username = null;
        pass_hash = null;
        menu_selection = 0;
        recipientList.clear();
        recievedMessages.clear();
        tertiary_state = 0;
        recipient = null;

        Menu();
    }
    
    DB_Connect(String filename, BufferedWriter bw){
        try{
            err = ErrorList();
            success = SuccessList();
            outToClient = bw;
            db_file = filename; 
            current_state = states[4];
            menu_selection = 0;
            Menu();
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }        
    }
    
    /**
     * This function directs all user input to their relevant destinations
     * @param input 
     */
    public static void checkInput(String input){
        System.out.println(current_state);
        switch(current_state){
            case "logged in" ->{
                if(tertiary_state == 0){
                    int action = GetAction(input);
                    if (action == (int) err.keySet().toArray()[0]) {
                        secondary_state = message_states[0];
                        WriteOut(err.get((int) err.keySet().toArray()[0]), false);
                    } else {
                        tertiary_state = action;
                    }
                    LoginScreen(null);                
                }else{
                    LoginScreen(input);                    
                }
                
                break;
            }
            
            //Directs the user through the process of logging in
            case "logging in" -> {
                boolean invalid_login = false;
                int login_success;
                if(secondary_state.equals(logging_in_states[0])){
                    login_success = getUser(input);
                    if (login_success == (int) success.keySet().toArray()[0]) {
                        System.out.println("Username - " + success.get(login_success));
                        UserLogin();
                    }else{
                        username = "no";
                        //The user will be unaware that they've entered incorrect credentials until after both the password and username has been supplied
                        invalid_login = true;
                        UserLogin();
                    }                    
                }else if(secondary_state.equals(logging_in_states[1])){
                    login_success = getPass(input);
                    if (login_success == (int) success.keySet().toArray()[0] && !username.equals("no")) {
                        System.out.println("Password - " + success.get(login_success));
                        DB_Comparison();
                    }else{
                        //The user will be unaware that they've entered incorrect credentials until after both the password and username has been supplied
                        invalid_login = true; 
                    }        
                }
                //Resets all variables and redirects the user to the main menu
                if(invalid_login == true){
                    WriteOut(err.get((int) err.keySet().toArray()[1]), false);
                    StatesReset();
                }
                break;
            }
            
            //Directs the user through the process of setting up their account
            case "creating account" ->{
                int creation_success;
                if(secondary_state.equals(creating_account_states[0])){
                    creation_success = getUser(input);
                    if (err.keySet().contains(creation_success)) {
                        WriteOut(err.get(creation_success), true);
                    }else{
                        System.out.println(username);
                        System.out.println("Username creation - " + success.get(creation_success));
                        UserCreation();
                    }                    
                }else if(secondary_state.equals(creating_account_states[1])){
                    creation_success = getPass(input);
                    if (creation_success == (int) success.keySet().toArray()[0] && !username.equals("no")) {
                        System.out.println("Password  creation- " + success.get(creation_success));
                        DB_UserSetup();                        
                    }else{
                        WriteOut(err.get(creation_success), false);
                        WriteOut(getMessage(informative[3]), true);
                    }        
                }else if(secondary_state.equals(creating_account_states[2])){
                    DB_UserSetup();
                }
                break;
            }
            
            //Displays the menu text so the user is aware of their options
            case "signed out" -> {
                Menu();
                current_state = states[4];
                break;
            }
            
            //Directs the application based on user input provided in response to the main menu text
            case "on menu"->{
                if(menu_selection == 0) menu_selection = validateInputOnMain(input);
                System.out.println(menu_selection);
                if(err.containsKey(menu_selection)){
                    WriteOut(err.get(menu_selection), false);
                    StatesReset();
                }else{
                    switch(menu_selection){
                        case 1:{       
                            menu_selection = 1;
                            UserCreation();
                            break;
                        }
                        case 2:{
                            menu_selection = 2;
                            UserLogin();
                            break;
                        }
                        case 3:{
                            //Exits the application
                           WriteOut("Thank you and have a wonderful day!", false);
                           WriteOut("Goodbye", true);
                           try{
                               outToClient.close();
                           }catch(IOException ex){
                               System.out.printf("""
                                    \nClass: %s
                                    Message: %s
                                    DTTM: %s
                                    """, ex.getClass().getName(), ex.getMessage(), GetDTTM());
                           }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * When the user is directed to the main menu, this function is called
     * to determine what they would like to complete next in the application.
     * @param input
     * @return 
     */
    private static int validateInputOnMain(String input){
        Object[] temp_err = err.keySet().toArray();
        try{
            int selection = Integer.parseInt(input);
            if(selection > 3 || selection < 1){
                return (int)temp_err[0];
            }else{
                return selection;
            }
        }catch(NumberFormatException ex){
            switch(input){
                case "create new account", "new account", "create", "new" ->{
                    return 1;
                }
                case "log in", "sign in" ->{
                    return 2;
                }
                case "exit" ->{
                    return 3;
                }
                default ->{
                    return (int)temp_err[0];
                }
            }
        }
    }
    
    /**
     * This function primarily holds the texts for the preexitsting user login.
     * The user will be directed here twice, depending on which information they need to provide
     * This function is only called when the current_state is on states - logging in
     */
    private static void UserLogin(){
        System.out.println("UserLogin----");
        if (username == null){
            current_state = states[1];
            secondary_state = logging_in_states[0];
            WriteOut(getMessage(informative[0]), true);
            return;
        }
        
        if (pass_hash == null){
            secondary_state = logging_in_states[1];
            WriteOut(getMessage(informative[1]), true);

        }       
    }
    
    /**
     * This function primarily holds the texts for user creation and adding credentials to the database.
     * The user will be directed here twice, depending on which information they
     * need to provide.
     * This function is only called when the current_state is on states - creating account
     */
    private static void UserCreation(){
        System.out.println("UserCreation");
        if (username == null){
            current_state = states[2];
            secondary_state = creating_account_states[0];
            WriteOut("Let's get your account created", false);
            WriteOut(getMessage(informative[0]), false);
            WriteOut(getMessage(informative[2]), true);
            return;
        }

        if (pass_hash == null){
            secondary_state = creating_account_states[1];
            WriteOut(getMessage(informative[1]), false);
            WriteOut(getMessage(informative[3]), true);
        }  
    }
    
    /**
     * This function is called to validate that the input provided by the user 
     * follows the username conventions.
     * This function is called when either the logging_in_states is set to "obtaining username"
     * or the creating_account_states is set to "validating username"
     * @param userInput
     * @return 
     */
    private static int getUser(String userInput){
        System.out.println((int) success.keySet().toArray()[1]);
        Pattern p = Pattern.compile("^[a-zA-Z0-9]{4,20}$");
        Matcher m = p.matcher(userInput);
        if(m.matches()) username = userInput;
        if (username == null) return (int) err.keySet().toArray()[0];
        else return 0;
    }
    
    /**
     * This function is called to validate that the input provided by the user 
     * follows the password conventions.
     * This function is called when either the logging_in_states is set to "obtaining password"
     * or the creating_account_states is set to "setting password"
     * @param userInput
     * @return 
     */
    private static int getPass(String userInput){
        String temp = null;
        Pattern p = Pattern.compile("^[a-zA-Z0-9]{8,30}$");
        Matcher m = p.matcher(userInput);
        if (m.matches()) temp = userInput;
        if (temp == null) return (int) err.keySet().toArray()[0];
        else if (temp != null && username != null){
            pass_hash = EncryptPassword(temp);
        }
        return 0;
    }
    
    /**
     * This is a helper function which is returns some of the repeating output texts.
     * @param message
     * @return 
     */
    private static String getMessage(String message){
        String returnable = null;
//        private static final String[] infomative = {"get user", "get pass", "user invalid", "user constraints", "pass constraints", "logged in", "message created"};
        switch(message){
            case "get user" ->{
                returnable = "Please enter User id (ex: user123)";
                break;
            }
            
            case "get pass" ->{
                returnable = "Please enter password";
                break;
            }
            
            case "user constraints" ->{
                returnable = "Your username must be at least 4 characters long and must be composed of alphanumeric characters.";
                break;
            }
            
            case "pass constraints" ->{
                //MD5 hashing
                returnable = "Your password must be at least 8 characters long and must be composed of alphanumeric characters.";
                break;
            }
//            
            case "upload success" ->{
                returnable = "You message has been successfully uploaded to the database";
                break;
            }
            
            case "db add error" ->{
                returnable = "Error adding account information to database. Check database connector and server logs.";
                break;
            }
            
            case "message upload error" ->{
                returnable = "Error writing message to database. Try again later";
                break;
            }
            
            case "message constraints" ->{
                returnable = "You may now compose your message. (NOTE: 250 characters max)";
                break;
            }
        }
        return returnable;
    }
    
    /**
     * This is the call to the main menu, which simply reads the menu text to the client.
     */
    private static void Menu(){
        String[] menu_text = {"Welcome to online Notetaker!!", "1. Create New Account", "2. Log In", "3. Exit", "Choice"};
        for(String message: menu_text){
            WriteOut(message, false);
        }
        WriteOut(null, true);
    }

    /**
     * This function sets up the error hashmap, which holds the possible errors that the user may run into
     * @return 
     */
    private static Map ErrorList(){ 
        Map<Integer, String> temp = new LinkedHashMap<>();
        temp.put(-10235, "Invalid input provided.");
        temp.put(-10557, "Invalid username/password.");
        temp.put(-10452, "Message length too long. Maximum of 250 characters allowed.");
        temp.put(-10636, "Unsuccessful upload to database.");
        temp.put(-10427, "Provided user account already exists. Continue from main menu");
        return temp;
    }
    
    /**
     * This function sets up the error hashmap, which holds the possible successful
     * messages that the user wil run into
     *
     * @return
     */
    private static Map SuccessList(){
        Map<Integer, String> temp = new LinkedHashMap<>();
        temp.put(0, "Process Successful");
        temp.put(1, "Obtaining username");
        temp.put(2, "Obtaining password");
        temp.put(3, "No messages available");

        return temp;
    }
    
    /**
     * The database call that creates a new user account within the database.
     * This function relies on prepared statements to avoid SQL Injection attacks.
     * After successful completion, the user is sent to the LoginScreen.
     */
    private static void DB_UserSetup(){
        secondary_state = creating_account_states[2];
        boolean exists = DB_UserExists("create");
        if(!exists){
            String query = "Insert into login(uname, password, last_login) values(?,?,?);";
            try (
                    Connection conn = getDBConnection(); PreparedStatement p_stmt = conn.prepareStatement(query);) {
                p_stmt.setString(1, username);
                p_stmt.setString(2, pass_hash);
                p_stmt.setString(3, GetDTTM());
                p_stmt.executeUpdate();
                current_state = states[0];
                secondary_state = message_states[0];
                pass_hash = null;
                LoginScreen(null);
            } catch (SQLException | IOException ex) {
                System.out.printf("""
                              \nClass: %s
                              Message: %s
                              DTTM: %s
                              """, ex.getClass().getName(), ex.getMessage(), GetDTTM());
                StatesReset();
            }
        }else{
            WriteOut(err.get((int) err.keySet().toArray()[4]), false);
            System.out.println(err.toString());
            StatesReset();
        }        
    }
    
    /**
     * This function is used by both the creation track and the login track to verify
     * if a particular identity is within the database.
     * 
     * If it's called by the creation track, then only the username will be required to verify
     * if the identity exists. Otherwise, both the username and password are required.
     * @param type
     * @return 
     */
    private static boolean DB_UserExists(String type){
        String query = null;
        switch(type){
            case "create" ->{
                query = "Select True as Found From login where uname = ?";
                break;
            }
            
            case "login" ->{
                query = "Select True as Found From login where uname = ? and password = ?;";
                break;
            }
        }
        try (
                Connection conn = getDBConnection(); 
                PreparedStatement p_stmt = conn.prepareStatement(query);) {
            p_stmt.setString(1, username);
            if(type.equals("login")) p_stmt.setString(2, pass_hash);
            ResultSet rs = p_stmt.executeQuery();
            while (rs.next()) {
                return rs.getBoolean("Found");
            }
        } catch (SQLException | IOException ex) {
            System.out.printf("""
                              Class: %s
                              Message: %s
                              DTTM: %s
                              """, ex.getClass().getName(), ex.getMessage(), GetDTTM());
        }
        return false;
    }
    
    /**
     * This function is responsible for validating that a provided user already
     * exists within the database.
     * If the user doesn't exist, then all variables are reset and the user is sent
     * back to the main menu. Otherwise, they are sent to the login screen.
     */
    private static void DB_Comparison(){
        secondary_state = logging_in_states[2];
        Object[] temp_err = err.keySet().toArray();
        boolean found = DB_UserExists("login");
        
        if(found){
            System.out.println("true");
            current_state = states[0];
            secondary_state = message_states[0];
            pass_hash = null;
            LoginScreen(null);
        }else{
            WriteOut(err.get((int)temp_err[1]) + "/n", false);
            StatesReset();
        }      
        
    }
            
    /**
     * This function takes a provided input (specifically a password) and 
     * encrypts it using MD5, before saving the encrypted password in the pass_hash variable
     * @param input
     * @return 
     */
    private static String EncryptPassword(String input){
        try{
            byte[] encoded_password = input.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(encoded_password);
            StringBuilder sb = new StringBuilder();
            for (byte d_byte : digest) {
                sb.append(d_byte);
            }
            return sb.toString();
        }catch(NoSuchAlgorithmException | UnsupportedEncodingException ex){
            String dttm = GetDTTM();
            System.out.printf("""
                               Exception: %s;
                               Details: %s;
                               Time: %s\n""", ex.getClass().getName(), ex.getMessage(), dttm);
            return null;
        }        
    }
    
    /**
     * Simply returns a formatted date\time object
     * @return 
     */
    private static String GetDTTM(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Simply sets up the database connection
     * @return
     * @throws IOException 
     */
    private static Connection getDBConnection() throws IOException{
        String database = db_file;
        try {
            return DriverManager.getConnection(database);
        } catch (SQLException ex) {
            String dttm = GetDTTM();
            System.out.printf("""
                               Exception: %s;
                               Details: %s;
                               Time: %s\n""", ex.getClass().getName(), ex.getMessage(), dttm);
            return null;
        }
    }
    
    /**
     * In charge of writing to the bufferedReader. This function also requires
     * that the user passes through an argument indicating if the contents of the BufferedWriter
     * is ready to be sent to the client
     * @param message
     * @param flush 
     */
    protected static void WriteOut(String message, boolean flush) {
        try {
            if(message != null){
                outToClient.write(message);
                outToClient.newLine();
            }            
            if(flush){
                WriteOut(" ", false);
                outToClient.flush();
            }
        } catch (IOException ex) {
            System.out.println("Error Writing to Client!");
        }
    }
    
    /**
     * Primary Login Screen.
     * This function is in charge of determining user interaction after logging in.
     */
    private static void LoginScreen(String input){
        System.out.printf("""
                          Secondary State: %s
                          Tertiary State: %d\n""", secondary_state, tertiary_state);
//                private static final String[] message_states = {"main menu", "reading messages", "composing message", "uploading to database", "obtaining user input", "selecting recipient"};

        switch(secondary_state){
            case "main menu"->{
                //Reads the message related options back to the client
                String[] login_menu = {"Message Menu", "--------------------", "1. Compose Message", "2. View Messages", "3. Exit"};
                for(String menu: login_menu){
                    WriteOut(menu, false);
                }
                WriteOut(null, true);
                secondary_state = message_states[4];
                tertiary_state = 0;
                recipient = null;
                break;
            }
            case "reading messages" ->{
                System.out.println("Can we read the messages?");
                DB_DisplayMessages();
                System.out.println("Did you read them yet?");
                secondary_state = message_states[0];
                tertiary_state = 0;
                LoginScreen(null);
                break;
            }
            case "selecting recipient"->{
                //Responsible for validating that the recipient the user selected already has an account.
                //Otherwise an error is returned
                System.out.println("Selecting recipient");
                int valid = ValidateRecipient(input);
                System.out.println("Valid: " + valid);
                if (err.keySet().contains(valid)) {
                    secondary_state = message_states[4];
                    tertiary_state = 1;
                    WriteOut(err.get(valid) + " Try again.", true);
                } else {
                    System.out.println("Stand up");
                    recipient = recipientList.get(valid);
                    secondary_state = message_states[3];
                    System.out.println(recipient);
                    WriteOut(getMessage(informative[8]), true);
                }
                break;
            }
            case "uploading to database" ->{
                System.out.println("Input: " + input);
                int valid_length = ValidateMessageLength(input);
                if (err.keySet().contains(valid_length)) {
                    WriteOut(err.get(valid_length), false);
                    System.out.println("User message is too long");
                } else {
                    user_message = input;
                    System.out.println(user_message);
                    System.out.println("Ok!!! Did we make it here?");
                    int successful_upload = DB_NoteUploader();
                    System.out.println("Ok!!! How about here?");
                    if (successful_upload == (int) err.keySet().toArray()[3]) {
                        System.out.println(err.get(3));
                        WriteOut(getMessage(informative[7]), false);
                    } else {
                        WriteOut(getMessage(informative[6]), false);
                    }
                    current_state = states[0];
                    secondary_state = message_states[0];
                    tertiary_state = 0;
                    LoginScreen(null);
                }
                break;
            }
            case "obtaining user input" ->{
                //This section determines what process the user wants to follow after reaching the LoginScreen
                System.out.println("Are you stuck?");
                switch(tertiary_state){
                    case 1 -> {
                        secondary_state = message_states[5];
                        if(recipient == null){
                            DB_getRecipients();
                            SelectRecipient();
                        }
                    }
                    case 2 -> {
                        secondary_state = message_states[1];
                        LoginScreen(null);
                    }
                    case 3 -> {
                        StatesReset();
                    }
                }
            }
        }
    }
    
    /**
     * Obtains the actions that the user will take while on the LoginScreen.
     * @param input
     * @return 
     */
    private static int GetAction(String input){
        Object[] temp_err = err.keySet().toArray();
        try{
            int selection = Integer.parseInt(input);
            if (selection > 3 || selection < 1) {
                return (int) temp_err[0];
            } else {
                return selection;
            }
        }catch (NumberFormatException ex) {
//            "1. Compose Message", "2. View Messages", "3. Exit"
            switch (input) {
                case "compose", "compose message" -> {
                    return 1;
                }
                case "view", "view message" -> {
                    return 2;
                }
                case "exit" -> {
                    return 3;
                }
                default -> {
                    return (int) temp_err[0];
                }
            }
        }
    }
        
    /**
     * This function gathers the list of unique users and stores them in an arraylist.
     * The arraylist is later used to help the user choose a recipient for their message.
     */
    private static void DB_getRecipients(){
        recipientList.clear();
        recipientList.add("Select your recipient: ");
        String query = "Select uname from login";
        try (
                Connection conn = getDBConnection(); 
                PreparedStatement p_stmt = conn.prepareStatement(query);) 
        {
            ResultSet rs = p_stmt.executeQuery();
            while (rs.next()) {
                recipientList.add(rs.getString("uname"));
            }
        } catch (SQLException | IOException ex) {
            System.out.printf("""
                              Class: %s
                              Message: %s
                              DTTM: %s
                              """, ex.getClass().getName(), ex.getMessage(), GetDTTM());
            WriteOut(getMessage(informative[6]), false);
        }
    }
    
    /**
     * Displays the list of potential recipients.
     * This function ensures that the user selects a recipient who has an account within the notetaker.
     */
    private static void SelectRecipient(){
        int i = 0;
        String temp;
        String[] temp_arr = new String[recipientList.size()];
        System.out.println(recipientList.size());
        for(String r: recipientList){
            if(i==0) temp = String.format("%s", r);
            else temp = String.format("%d. %s", i, r);
            temp_arr[i]= temp;
            System.out.println(temp);
            i++;
        }
        for(String s: temp_arr){
            WriteOut(s, false);
        }
        WriteOut(null, true);
    }
    
    /**
     * Recieves the use input and checks if it related to an individual in the recipient list.
     * If not, an error is returned. Otherwise, the index of the recipient is returned.
     * @param input
     * @return 
     */
    private static int ValidateRecipient(String input){
        Object[] temp_err = err.keySet().toArray();
        try {
            int selection = Integer.parseInt(input);
            if (selection >= recipientList.size() || selection < 0) {
                System.out.println(selection);
                return (int) temp_err[0];
            } else {
                System.out.println("Uh oh!!!");
                return selection;
            }
        } catch (NumberFormatException ex) {
            System.out.println("Did I skip to the erro?");
            if(recipientList.contains(input)){
                return recipientList.indexOf(input);
            }else{
                return (int) temp_err[0];
            }
        }
    }
    
    private static int ValidateMessageLength(String input){
        Object[] temp_err = err.keySet().toArray();
        Object[] temp_success = success.keySet().toArray();

        if(input.length() > 250){
            return (int) temp_err[2];
        }
        else return (int) temp_success[0];
    }
    
    /**
     * This function is responsible for displaying messages to the user.
     * It reads the database for entries where the logged in user is the recipient of the message.
     * It then add those messages to an arrayList and finally reads directly from the arraylist
     */
    private static void DB_DisplayMessages(){
        System.out.println("Why didn't I add a debugging message here?");
        String query = "Select sender, message, date_composed from messages as m inner join login as l on l.uname=m.recipient where l.uname = ?;";
        Object[] temp_err = err.keySet().toArray();
        try (
                Connection conn = getDBConnection(); 
                PreparedStatement p_stmt = conn.prepareStatement(query)
            ;) {
            p_stmt.setString(1, username);
            ResultSet rs = p_stmt.executeQuery();
            while (rs.next()) {
                String temp = String.format("%s; %s(%s)",rs.getString("message"), rs.getString("sender"), rs.getString("date_composed"));
                recievedMessages.add(temp);
            }
            if(recievedMessages.isEmpty()) WriteOut(success.get(3), false);
            else{
                for(String msg: recievedMessages){
                    WriteOut(msg, false);
                }
                WriteOut("================================================", false);
            }
            recievedMessages.clear();

        } catch (SQLException | IOException ex) {
            System.out.printf("""
                              Class: %s
                              Message: %s
                              DTTM: %s
                              """, ex.getClass().getName(), ex.getMessage(), GetDTTM());
            WriteOut(getMessage(informative[6]), false);
            StatesReset();
        }
    }
    
    /**
     * This function is in charge of 
     * @return 
     */
    private static int DB_NoteUploader(){
        String query = "Insert into messages(sender, recipient, message, date_composed) values(?, ?, ?, ?);";
        Object[] temp_err = err.keySet().toArray();
        try (
                Connection conn = getDBConnection(); 
                PreparedStatement p_stmt = conn.prepareStatement(query);
            ) {
            boolean success = false;
            p_stmt.setString(1, username);
            p_stmt.setString(2, recipient);
            p_stmt.setString(3, user_message);
            p_stmt.setString(4, GetDTTM());

            int rs = p_stmt.executeUpdate();
            if(rs > 0) {
                return rs;
            } else {
                return (int) temp_err[3];
            }
        } catch (SQLException | IOException ex) {
            System.out.printf("""
                              Class: %s
                              Message: %s
                              DTTM: %s
                              """, ex.getClass().getName(), ex.getMessage(), GetDTTM());
            WriteOut(getMessage(informative[6]), false);
            return (int) temp_err[3];
        }
    }
}
