import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.json.JSONObject;

public class Recommender {
    private JFrame frame;
    private JTextField textField;

    // Steam API Key
    static final String KEY = "5F0FFF1D55D35CE91DD512BC9343A97E"; 
    private JLabel introFriendsLbl;
    private JLabel imgOneLblOne, recCountryLbLOne, recCountryOne, recUsernameOne, recUsernameLblOne;
    private JLabel imgOneLblTwo, recCountryLbLTwo, recCountryTwo, recUsernameTwo, recUsernameLblTwo;
    private JLabel imgOneLblThr, recCountryLbLThr, recCountryThr, recUsernameThr, recUsernameLblThr;
    private JLabel imgOneLblFou, recCountryLbLFou, recCountryFou, recUsernameFou, recUsernameLblFou;
    private JLabel imgOneLblFiv, recCountryLbLFiv, recCountryFiv, recUsernameFiv, recUsernameLblFiv;
	private JButton recVisitButtonOne, recVisitButtonTwo, recVisitButtonThr, recVisitButtonFou, 
																				 recVisitButtonFiv;
	private JSeparator separatorOne, separatorTwo, separatorThr, separatorFou;

    /**
    * Launch the application.
    */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Recommender window = new Recommender();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
       * Gets the response from the API request as a JSONObjects
       * 
       * @param requrestURLHeader, beginning of API request URL
       * @param requestURLFooter, ending of API request URL
       * @param userID, steam ID of user 
       * @param isPlural, true if URL should have "steamids" and false if URL should have "steamid"
       * 
       * @return JSONObject of response from API request, null if request was unsuccessful
       *
       */
    public JSONObject getJSONResponse(String requestURLHeader, 
            String requestURLFooter, String userID, boolean isPlural) {
        
        try {
            String formattedRequestURL;
            
            if (isPlural) {
                formattedRequestURL = requestURLHeader + KEY + "&steamids=" + 
                        userID + requestURLFooter;
            } else {
                formattedRequestURL = requestURLHeader + KEY + "&steamid=" + 
                        userID + requestURLFooter;
            }
            
            System.out.println(formattedRequestURL);
            URL request = null;
            
            try {
                request = new URL(formattedRequestURL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            
            try {
                URLConnection connection = request.openConnection();
                connection.setDoOutput(true); 
            } catch (IOException e) {
                e.printStackTrace();
            }   
            
            JSONObject obj = null;
            
            try {
                Scanner sc = new Scanner(request.openStream());
                StringBuilder jsonStr = new StringBuilder();
                while (sc.hasNext()) {
                    jsonStr.append(sc.next());
                }
                obj = new JSONObject(jsonStr.toString());
                sc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
      
      /**
       * Gets the top 5 friends of input user's friends that have the
       * most play time of the input user's top 10 most played games on Steam
       * 
       * @param userID, Steam 64 ID
       * 
       * @return List of the profile information of the top 5 recommendations,
       * including username, profile page URL, profile picture and country
       *
       */
      
    public List<String[]> getTop5Recs(String userID) {
        //JSONObject of games owned by steam user with input ID
        JSONObject userGamesObj = getJSONResponse("http://api.steampowered.com/IPlayerService/"
                + "GetOwnedGames/v0001/?key=","&include_played_free_games=1", userID, false);
        
        //Creates gamesMap which contains the top 10 most played games by user with input ID
        Map<String, Integer> gamesMap = new HashMap<>();
        
        for (int i = 0; i < userGamesObj.getJSONObject("response")
                .getJSONArray("games").length(); i++) {
            
            int playTime = userGamesObj.getJSONObject("response")
                    .getJSONArray("games").getJSONObject(i).getInt("playtime_forever");
            
            if (playTime != 0) {
                //finds min. play time in gamesMap
                Iterator iter = gamesMap.entrySet().iterator();
                String minGameID = null;
                Integer minGamePlayTime = Integer.MAX_VALUE;
                
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry)iter.next();
                    if ((Integer)entry.getValue() < minGamePlayTime) {
                        minGameID = (String)entry.getKey();
                        minGamePlayTime = (Integer)entry.getValue();
                    }
                }  
                
                String gameID = "" + userGamesObj.getJSONObject("response")
                    .getJSONArray("games").getJSONObject(i).getInt("appid");
                
                if (gamesMap.size() >= 10) {
                    if (playTime > minGamePlayTime) {
                        gamesMap.remove(minGameID);
                        gamesMap.put(gameID, playTime);
                    }
                } else {
                    gamesMap.put(gameID, playTime);
                }
            }
        }
        
        //Creates footer of URL using gamesMap that will serve as a filter for games of other users
        Iterator iter = gamesMap.entrySet().iterator();
        int x = 0;
        StringBuilder URLFooter = new StringBuilder();
        
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            URLFooter.append("&appids_filter[" + x + "]=" + entry.getKey());
            
            x++;
        }
        
        URLFooter.append("&include_played_free_games=1");
        
        //JSONObject of friends of steam user with input ID
        JSONObject userFriendsObj = getJSONResponse("http://api.steampowered.com/ISteamUser/"
                + "GetFriendList/v0001/?key=", "", userID, false);

        //set of IDs of friends of user with input ID
        Set<String> friendsSet = new HashSet<>();
        
        for (int i = 0; i < userFriendsObj.getJSONObject("friendslist")
                .getJSONArray("friends").length(); i++) {
            
            String friendID = userFriendsObj.getJSONObject("friendslist")
                    .getJSONArray("friends").getJSONObject(i).getString("steamid");
            friendsSet.add(friendID);
        }
        
        // Creates friendsFriendMap which contains the top 5 friends
        // of user's friends with most play time of user's top 10 most played games
        Map<String, Integer> friendsFriendMap = new HashMap<>();
        
        for (int i = 0; i < userFriendsObj.getJSONObject("friendslist")
                .getJSONArray("friends").length(); i++) {
            
            String friendID = userFriendsObj.getJSONObject("friendslist")
                    .getJSONArray("friends").getJSONObject(i).getString("steamid");
            
            //JSONObject of friends of steam user with friendID
            JSONObject friendsFriendsObj = getJSONResponse("http://api.steampowered.com/ISteamUser/"
                    + "GetFriendList/v0001/?key=", "", friendID, false);
            
            if (friendsFriendsObj != null) {
                for (int j = 0; j < friendsFriendsObj.getJSONObject("friendslist")
                        .getJSONArray("friends").length(); j++) {
                    
                    String friendsFriendID = friendsFriendsObj.getJSONObject("friendslist")
                            .getJSONArray("friends").getJSONObject(j).getString("steamid");
                    
                    // JSONObject of games owned by steam user with friendsFriendID,
                    // filtered by user's top 10 most played games
                    JSONObject friendsFriendGamesObj = getJSONResponse("http://api.steampowered.com"
                            + "/IPlayerService/GetOwnedGames/v0001/?key="
                            , URLFooter.toString(), friendsFriendID, false);
                    
                    // checks to see if JSON response has games property 
                    // and friendFriendID isn't friend of user
                    if (friendsFriendGamesObj != null && friendsFriendGamesObj.
                            getJSONObject("response").has("games") && 
                            !friendsSet.contains(friendsFriendID)) {
                        
                        //sums up play time
                        int friendsFriendPlayTime = 0;
                        
                        for (int k = 0; k < friendsFriendGamesObj.getJSONObject("response").
                                getJSONArray("games").length(); k++) {
                            
                            friendsFriendPlayTime += friendsFriendGamesObj.
                                    getJSONObject("response").getJSONArray("games").
                                    getJSONObject(k).getInt("playtime_forever");
                        }
                        
                        //finds min. play time in friendsFriendMap
                        Iterator iterFF = friendsFriendMap.entrySet().iterator();
                        String minFFID = null;
                        Integer minFFPlayTime = Integer.MAX_VALUE;
                        
                        while (iterFF.hasNext()) {
                            Map.Entry entry = (Map.Entry)iterFF.next();
                            if ((Integer)entry.getValue() < minFFPlayTime) {
                                minFFID = (String)entry.getKey();
                                minFFPlayTime = (Integer)entry.getValue();
                            }
                        }
                        
                        if (friendsFriendMap.size() >= 5) {
                            if (friendsFriendPlayTime  > minFFPlayTime) {
                                friendsFriendMap.remove(minFFID);
                                friendsFriendMap.put(friendsFriendID, friendsFriendPlayTime);
                            }

                        } else {
                            friendsFriendMap.put(friendsFriendID, friendsFriendPlayTime);
                        }
                    } 
                }
            }
        }
        //sorts entries of friendsFriendMap by play time
        List<Map.Entry<String, Integer>> list = new 
                ArrayList<Map.Entry<String, Integer>>(friendsFriendMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        
        List<String[]> topRecs = new LinkedList<String[]>();
        
        for (int y = list.size() - 1; y >= 0; y--) {
            
            //JSONObject of steam profile
            JSONObject userProfileObj = getJSONResponse("http://api.steampowered.com/ISteamUser/"
                    + "GetPlayerSummaries/v0002/?key=", "", (String) list.get(y).getKey(), true);
            
            String username = userProfileObj.getJSONObject("response").getJSONArray("players").
                    getJSONObject(0).getString("personaname");
            String profileURL = userProfileObj.getJSONObject("response").getJSONArray("players").
                    getJSONObject(0).getString("profileurl");
            String profilePicURL = userProfileObj.getJSONObject("response").getJSONArray("players").
                    getJSONObject(0).getString("avatarfull");
            String country = "";
            
            if (userProfileObj.getJSONObject("response").getJSONArray("players").
                    getJSONObject(0).has("loccountrycode")) {
                
                country = userProfileObj.getJSONObject("response").getJSONArray("players").
                        getJSONObject(0).getString("loccountrycode");
            }
            
            topRecs.add(new String[]{username, profileURL, profilePicURL, country});
        }

        return topRecs;
    }
          
    public Recommender() {
        initialize();
    }
    
    /**
     * Building the GUI and initializing the algorithm instance
     */
    private void initialize() {
        
        frame = new JFrame("Steam Friend Recommender");
        frame.setBounds(100, 100, 315, 520);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.getContentPane().setLayout(null);
        
        JLabel enteredId = new JLabel("Enter Your Steam 64 ID:");
        enteredId.setBounds(10, 11, 144, 20);
        frame.getContentPane().add(enteredId);
        
        textField = new JTextField();
        textField.setBounds(10, 42, 144, 20);
        frame.getContentPane().add(textField);
        textField.setColumns(10);
        
        JButton startAlgoBtn = new JButton("Find Friends");
        startAlgoBtn.setBounds(164, 41, 126, 23);
        frame.getContentPane().add(startAlgoBtn);
        
        JLabel loadingLabel = new JLabel("LOADING...");
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(36f));
        loadingLabel.setBounds(50, 150, 200, 100);
        frame.getContentPane().add(loadingLabel);
        loadingLabel.setVisible(false);
        
        ImageIcon loading = new ImageIcon("ajax-loader.gif");
        Image loadingImg = loading.getImage().getScaledInstance(200,200,Image.SCALE_DEFAULT);
        JLabel gifLabel = new JLabel(new ImageIcon(loadingImg));
        gifLabel.setBounds(50, 150, 200, 200);
        frame.getContentPane().add(gifLabel);
        gifLabel.setVisible(false);
        
        //listener for button
        startAlgoBtn.addActionListener(p -> {
            gifLabel.setVisible(true);
            loadingLabel.setVisible(true);
            //Removes old previous top 5 recommended friends
            if (imgOneLblOne != null && recCountryLbLOne != null && recVisitButtonOne != null 
            		&& recCountryOne != null && recUsernameOne != null && recUsernameLblOne != null 
            													&& introFriendsLbl != null) {

				frame.remove(imgOneLblOne);
				frame.remove(recCountryLbLOne);
				frame.remove(recVisitButtonOne);
				frame.remove(recCountryOne);
				frame.remove(recUsernameOne);
				frame.remove(recUsernameLblOne);
				frame.remove(introFriendsLbl);
				frame.repaint();
				frame.revalidate();
			}
            if (imgOneLblTwo != null && recCountryLbLTwo != null && recVisitButtonTwo != null 
            		&& recCountryTwo != null && recUsernameTwo != null && recUsernameLblTwo != null
            							&& separatorOne != null) {
				frame.remove(imgOneLblTwo);
				frame.remove(recCountryLbLTwo);
				frame.remove(recVisitButtonTwo);
				frame.remove(recCountryTwo);
				frame.remove(recUsernameTwo);
				frame.remove(recUsernameLblTwo);
				frame.remove(separatorOne);
				frame.repaint();
				frame.revalidate();
			}
            if (imgOneLblThr != null && recCountryLbLThr != null && recVisitButtonThr != null 
            		&& recCountryThr != null && recUsernameThr != null && recUsernameLblThr != null
            							&& separatorTwo != null) {
				frame.remove(imgOneLblThr);
				frame.remove(recCountryLbLThr);
				frame.remove(recVisitButtonThr);
				frame.remove(recCountryThr);
				frame.remove(recUsernameThr);
				frame.remove(recUsernameLblThr);
				frame.remove(separatorTwo);
				frame.repaint();
				frame.revalidate();
			}
            if (imgOneLblFou != null && recCountryLbLFou != null && recVisitButtonFou != null 
            		&& recCountryFou != null && recUsernameFou != null && recUsernameLblFou != null
            							&& separatorThr != null) {
				frame.remove(imgOneLblFou);
				frame.remove(recCountryLbLFou);
				frame.remove(recVisitButtonFou);
				frame.remove(recCountryFou);
				frame.remove(recUsernameFou);
				frame.remove(recUsernameLblFou);
				frame.remove(separatorThr);
				frame.repaint();
				frame.revalidate();
			}
            if (imgOneLblFiv != null && recCountryLbLFiv != null && recVisitButtonFiv != null 
            		&& recCountryFiv != null && recUsernameFiv != null && recUsernameLblFiv != null
            							&& separatorFou != null) {
				frame.remove(imgOneLblFiv);
				frame.remove(recCountryLbLFiv);
				frame.remove(recVisitButtonFiv);
				frame.remove(recCountryFiv);
				frame.remove(recUsernameFiv);
				frame.remove(recUsernameLblFiv);
				frame.remove(separatorFou);
				frame.repaint();
				frame.revalidate();
			}
            
            //thread to run getTop5Recs concurrently
            new Thread(() -> {
                String theirID = textField.getText().trim(); 
                List<String[]> recs = getTop5Recs(theirID);
                //displays the top 5 recommendations output
                try {
                    String[] thisUser = recs.get(0);
                    Image bgImage = Toolkit.getDefaultToolkit().getImage(new URL(thisUser[2]));
                    ImageIcon convToIcon = new ImageIcon(bgImage);
                    Image scaledProfilePic = convToIcon.getImage().
                            getScaledInstance(60,60,Image.SCALE_DEFAULT);
                    imgOneLblOne = new JLabel(new ImageIcon(scaledProfilePic));
                    imgOneLblOne.setBounds(10, 120, 60, 60);
                    frame.getContentPane().add(imgOneLblOne);
                    
                    recUsernameLblOne = new JLabel("Username:");
                    recUsernameLblOne.setBounds(80, 119, 74, 14);
                    frame.getContentPane().add(recUsernameLblOne);
                    
                    recUsernameOne = new JLabel(thisUser[0]);
                    recUsernameOne.setBounds(80, 135, 140, 14);
                    frame.getContentPane().add(recUsernameOne);
                    
                    recCountryLbLOne = new JLabel("User's Country:");
                    recCountryLbLOne.setBounds(80, 151, 105, 14);
                    frame.getContentPane().add(recCountryLbLOne);

                    recCountryOne = new JLabel(thisUser[3]);
                    recCountryOne.setBounds(80, 166, 140, 14);
                    frame.getContentPane().add(recCountryOne);
                    
                    recVisitButtonOne = new JButton("Visit");
                    recVisitButtonOne.setBounds(230, 136, 60, 23);
                    frame.getContentPane().add(recVisitButtonOne);
                    recVisitButtonOne.addActionListener(e -> {
                        try {
                            Desktop.getDesktop().browse(new URL(thisUser[1]).toURI());
                        } catch (Exception q) {
                            q.printStackTrace();
                        }
                    });
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
                
                try {
                	separatorOne = new JSeparator();
                    separatorOne.setBackground(Color.GRAY);
                    separatorOne.setForeground(Color.GRAY);
                    separatorOne.setBounds(10, 184, 280, 20);
                    frame.getContentPane().add(separatorOne);
                    
                    String[] thisUser = recs.get(1);
                    Image bgImage = Toolkit.getDefaultToolkit().getImage(new URL(thisUser[2]));
                    ImageIcon swag = new ImageIcon(bgImage);
                    Image scaledProfilePic = swag.getImage().
                            getScaledInstance(60,60,Image.SCALE_DEFAULT);
                    imgOneLblTwo = new JLabel(new ImageIcon(scaledProfilePic));
                    imgOneLblTwo.setBounds(10, 190, 60, 60);
                    frame.getContentPane().add(imgOneLblTwo);

                    recUsernameLblTwo = new JLabel("Username:");
                    recUsernameLblTwo.setBounds(80, 189, 74, 14);
                    frame.getContentPane().add(recUsernameLblTwo);
                    
                    recUsernameTwo = new JLabel(thisUser[0]);
                    recUsernameTwo.setBounds(80, 205, 126, 14);
                    frame.getContentPane().add(recUsernameTwo);
                    
                    recCountryLbLTwo = new JLabel("User's Country:");
                    recCountryLbLTwo.setBounds(80, 221, 105, 14);
                    frame.getContentPane().add(recCountryLbLTwo);
                    
                    recCountryTwo = new JLabel(thisUser[3]);
                    recCountryTwo.setBounds(80, 236, 140, 14);
                    frame.getContentPane().add(recCountryTwo);
                    
                    recVisitButtonTwo = new JButton("Visit");
                    recVisitButtonTwo.setBounds(230, 211, 60, 23);
                    frame.getContentPane().add(recVisitButtonTwo);
                    recVisitButtonTwo.addActionListener(e -> {
                        try {
                            Desktop.getDesktop().browse(new URL(thisUser[1]).toURI());
                        } catch (Exception q) {
                            q.printStackTrace();
                        }
                    });
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
                
                try {
                	separatorTwo = new JSeparator();
                    separatorTwo.setForeground(Color.GRAY);
                    separatorTwo.setBackground(Color.GRAY);
                    separatorTwo.setBounds(10, 254, 280, 20);
                    frame.getContentPane().add(separatorTwo);
                    
                    String[] thisUser = recs.get(2);
                    Image bgImage = Toolkit.getDefaultToolkit().getImage(new URL(thisUser[2]));
                    ImageIcon swag = new ImageIcon(bgImage);
                    Image scaledProfilePic = swag.getImage().
                            getScaledInstance(60,60,Image.SCALE_DEFAULT);
                    imgOneLblThr = new JLabel(new ImageIcon(scaledProfilePic));
                    imgOneLblThr.setBounds(10, 261, 60, 60);
                    frame.getContentPane().add(imgOneLblThr);
                    
                    recUsernameLblThr = new JLabel("Username:");
                    recUsernameLblThr.setBounds(80, 261, 74, 14);
                    frame.getContentPane().add(recUsernameLblThr);
                    
                    recUsernameThr = new JLabel(thisUser[0]);
                    recUsernameThr.setBounds(80, 277, 126, 14);
                    frame.getContentPane().add(recUsernameThr);
                    
                    recCountryLbLThr = new JLabel("User's Country:");
                    recCountryLbLThr.setBounds(80, 293, 105, 14);
                    frame.getContentPane().add(recCountryLbLThr);
                    
                    recCountryThr = new JLabel(thisUser[3]);
                    recCountryThr.setBounds(80, 308, 140, 14);
                    frame.getContentPane().add(recCountryThr);
                    
                    recVisitButtonThr = new JButton("Visit");
                    recVisitButtonThr.setBounds(230, 280, 60, 23);
                    frame.getContentPane().add(recVisitButtonThr);
                    recVisitButtonThr.addActionListener(e -> {
                        try {
                            Desktop.getDesktop().browse(new URL(thisUser[1]).toURI());
                        } catch (Exception q) {
                            q.printStackTrace();
                        }
                    });
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                
                try {
                	separatorThr = new JSeparator();
                    separatorThr.setForeground(Color.GRAY);
                    separatorThr.setBackground(Color.GRAY);
                    separatorThr.setBounds(10, 325, 280, 20);
                    frame.getContentPane().add(separatorThr);
                    
                    String[] thisUser = recs.get(3);
                    Image bgImage = Toolkit.getDefaultToolkit().getImage(new URL(thisUser[2]));
                    ImageIcon swag = new ImageIcon(bgImage);
                    Image scaledProfilePic = swag.getImage().
                            getScaledInstance(60,60,Image.SCALE_DEFAULT);
                    imgOneLblFou = new JLabel(new ImageIcon(scaledProfilePic));
                    imgOneLblFou.setBounds(10, 332, 60, 60);
                    frame.getContentPane().add(imgOneLblFou);
                    
                    recUsernameLblFou = new JLabel("Username:");
                    recUsernameLblFou.setBounds(80, 332, 74, 14);
                    frame.getContentPane().add(recUsernameLblFou);
                    
                    recUsernameFou = new JLabel(thisUser[0]);
                    recUsernameFou.setBounds(80, 348, 126, 14);
                    frame.getContentPane().add(recUsernameFou);
                    
                    recCountryLbLFou = new JLabel("User's Country:");
                    recCountryLbLFou.setBounds(80, 364, 105, 14);
                    frame.getContentPane().add(recCountryLbLFou);
                    
                    recCountryFou = new JLabel(thisUser[3]);
                    recCountryFou.setBounds(80, 379, 140, 14);
                    frame.getContentPane().add(recCountryFou);
                    
                    recVisitButtonFou = new JButton("Visit");
                    recVisitButtonFou.setBounds(230, 351, 60, 23);
                    frame.getContentPane().add(recVisitButtonFou);
                    recVisitButtonFou.addActionListener(e -> {
                        try {
                            Desktop.getDesktop().browse(new URL(thisUser[1]).toURI());
                        } catch (Exception q) {
                            q.printStackTrace();
                        }
                    });
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
                
                try {
                	separatorFou = new JSeparator();
                    separatorFou.setForeground(Color.GRAY);
                    separatorFou.setBackground(Color.GRAY);
                    separatorFou.setBounds(10, 396, 280, 20);
                    frame.getContentPane().add(separatorFou);
                    
                    String[] thisUser = recs.get(4);
                    Image bgImage = Toolkit.getDefaultToolkit().getImage(new URL(thisUser[2]));
                    ImageIcon swag = new ImageIcon(bgImage);
                    Image scaledProfilePic = swag.getImage().
                            getScaledInstance(60,60,Image.SCALE_DEFAULT);
                    imgOneLblFiv = new JLabel(new ImageIcon(scaledProfilePic));
                    imgOneLblFiv.setBounds(10, 403, 60, 60);
                    frame.getContentPane().add(imgOneLblFiv);
                    
                    recUsernameLblFiv = new JLabel("Username:");
                    recUsernameLblFiv.setBounds(80, 403, 74, 14);
                    frame.getContentPane().add(recUsernameLblFiv);
                    
                    recUsernameFiv = new JLabel(thisUser[0]);
                    recUsernameFiv.setBounds(80, 419, 126, 14);
                    frame.getContentPane().add(recUsernameFiv);
                    
                    recCountryLbLFiv = new JLabel("User's Country:");
                    recCountryLbLFiv.setBounds(80, 435, 105, 14);
                    frame.getContentPane().add(recCountryLbLFiv);
                    
                    recCountryFiv = new JLabel(thisUser[3]);
                    recCountryFiv.setBounds(80, 450, 140, 14);
                    frame.getContentPane().add(recCountryFiv);
                    
                    recVisitButtonFiv = new JButton("Visit");
                    recVisitButtonFiv.setBounds(230, 421, 60, 23);
                    frame.getContentPane().add(recVisitButtonFiv);
                    recVisitButtonFiv.addActionListener(e -> {
                        try {
                            Desktop.getDesktop().browse(new URL(thisUser[1]).toURI());
                        } catch (Exception q) {
                            q.printStackTrace();
                        }
                    });
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
                
                introFriendsLbl = new JLabel("Your Top 5 Recomended Friends:");
                introFriendsLbl.setBounds(10, 73, 210, 14);
                frame.getContentPane().add(introFriendsLbl);
                
                gifLabel.setVisible(false);
                loadingLabel.setVisible(false);
                frame.repaint();
                frame.revalidate();
            }).start();
        });
    }
}
