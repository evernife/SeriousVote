package net.adamsanchez.seriousvote;

import ninja.leaping.configurate.ConfigurationNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by adam_ on 01/28/17.
 */
public class ConfigManager {
    ConfigurationNode rootNode;

    public ConfigManager(ConfigurationNode node){
        this.rootNode = node;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////
    public List<String> getWeeklySetCommands(ConfigurationNode node){
        return node.getNode("config","milestones","weekly","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    public List<String> getMonthlySetCommands(ConfigurationNode node){
        return node.getNode("config","milestones","monthly","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    public List<String> getYearlySetCommands(ConfigurationNode node){
        return node.getNode("config","milestones","yearly","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    public boolean getMilestonesEnabled(ConfigurationNode node){
        return node.getNode("config","milestones","enabled").getBoolean();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////
    public String getDatabaseName(ConfigurationNode node){
        return node.getNode("config","database","name").getString();
    }
    public String getDatabaseHostname(ConfigurationNode node){
        return node.getNode("config","database","hostname").getString();
    }
    public String getDatabasePort(ConfigurationNode node){
        return node.getNode("config","database","port").getString();
    }
    public String getDatabasePrefix(ConfigurationNode node){
        return node.getNode("config","database","prefix").getString();
    }
    public String getDatabaseUsername(ConfigurationNode node){
        return node.getNode("config","database","username").getString();
    }
    public String getDatabasePassword(ConfigurationNode node){
        return node.getNode("config","database","password").getString();
    }
  ////////////////////////////////////////////////////////////////////////////////////////////
    public List<String> getSetCommands(ConfigurationNode node) {
        return node.getNode("config","Rewards","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    public List<String> getRandomCommands(ConfigurationNode node) {
        return node.getNode("config","Rewards","random").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    public List<String> getVoteSites(ConfigurationNode node) {
        //TODO code potentially breaking here -- investigate
        return node.getNode("config","vote-sites").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }

    //Returns the string value from the Config for the public message. This must be deserialized
    public String getPublicMessage(ConfigurationNode node){
        return node.getNode("config","broadcast-message").getString();
    }
    public int getRewardsNumber(ConfigurationNode node){

        int number = node.getNode("config", "random-rewards-number").getInt();
        return number;
    }
    public int getMaxRewardsNumber(ConfigurationNode node){
        int number = node.getNode("config", "rewards-max").getInt() + 1;
        return number;
    }
    public int getMinRewardsNumber(ConfigurationNode node){
        int number = node.getNode("config", "rewards-min").getInt();
        return number;
    }
    public boolean getIsNoRandom(ConfigurationNode node){
        int number = node.getNode("config", "random-rewards-number").getInt();
        return number == 0? true:false;
    }


}
