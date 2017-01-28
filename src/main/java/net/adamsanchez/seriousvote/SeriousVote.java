package net.adamsanchez.seriousvote;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.sponge.event.VotifierEvent;


import jdk.nashorn.internal.runtime.regexp.joni.Config;
import ninja.leaping.configurate.ConfigurationNode;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;

import org.spongepowered.api.entity.living.player.Player;


import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.Listener;

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;

import org.spongepowered.api.plugin.PluginContainer;

import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;


import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Created by adam_ on 12/08/16.
 */
@SuppressWarnings("unused")
@Plugin(id = "seriousvote", name = "SeriousVote", version = "3.0", description = "This plugin enables server admins to give players rewards for voting for their server.", dependencies = @Dependency(id = "nuvotifier", version = "1.0", optional = false) )
public class SeriousVote
{

    @Inject private Game game;
    private Game getGame(){
        return this.game;
    }

    @Inject private PluginContainer plugin;
    private PluginContainer getPlugin(){
        return this.plugin;
    }
    @Inject
    private Metrics metrics;
    private static SeriousVote instance;

    private static SeriousVote seriousVotePlugin;


    @Inject  Logger logger;
    public Logger getLogger()
    {
        return logger;
    }


    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConfig;
    private Path offlineVotes;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path privateConfigDir;
    private CommentedConfigurationNode rootNode;

    ///////////////////////////////////////////////////////
    private Milestones milestones;
    public List<String> monthlySet, yearlySet, weeklySet;
    ///////////////////////////////////////////////////////
    public String databaseName, databaseHostname,databasePort,databasePrefix,databaseUsername,databasePassword;
    ///////////////////////////////////////////////////////
    private List<String> commandQueue = Collections.synchronizedList(new LinkedList<String>());

    LinkedHashMap<Integer, List<Map<String, String>>> lootMap = new LinkedHashMap<Integer, List<Map<String,String>>>();
    HashMap<UUID,Integer> storedVotes = new HashMap<UUID,Integer>();
    int randomRewardsNumber;
    int rewardsMin;
    int rewardsMax;
    int randomRewardsGen;
    List<String> setCommands;
    List<Integer> chanceMap;
    String currentRewards;
    String publicMessage;
    boolean hasLoot = false;
    boolean isNoRandom = false;
    private static Optional<UserStorageService> userStorage;
 //////////////////////////////////////////////////////////////////



    @Listener
    public void onInitialization(GamePreInitializationEvent event){
        instance = this;
        userStorage = Sponge.getServiceManager().provide(UserStorageService.class);

        //getLogger().info("Serious Vote loading...");
        game.getServer().getConsole().sendMessage(Text.of("Loading Serious Servers").toBuilder().color(TextColors.GOLD).build());
        getLogger().info("Trying To setup Config Loader");

        Asset configAsset = plugin.getAsset("seriousvote.conf").orElse(null);
        Asset offlineVoteAsset = plugin.getAsset("offlinevotes.dat").orElse(null);

        offlineVotes = Paths.get(privateConfigDir.toString(),"", "offlinevotes.dat");





        if (Files.notExists(defaultConfig)) {
            if (configAsset != null) {
                try {
                    getLogger().info("Copying Default Config");
                    getLogger().info(configAsset.readString());
                    getLogger().info(defaultConfig.toString());
                    configAsset.copyToFile(defaultConfig);
                } catch (IOException e) {
                    e.printStackTrace();
                    getLogger().error("Could not unpack the default config from the jar! Maybe your Minecraft server doesn't have write permissions?");
                    return;
                }
            } else {
                getLogger().error("Could not find the default config file in the jar! Did you open the jar and delete it?");
                return;
            }
        }

        if (Files.notExists(offlineVotes)){
            try {
                saveOffline();
            } catch (IOException e) {
                getLogger().error("Could Not Initialize the offlinevotes file! What did you do with it");
                //getLogger().error(e.toString());
            }
        }
        currentRewards = "";


        reloadConfigs();

        //Begin Command Executor




    }


    @Listener
    public void onPostInitalization(GamePostInitializationEvent event){
        instance = this;
    }

    @Listener
    public void onServerStart(GameInitializationEvent event)
    {
        seriousVotePlugin = this;
        registerCommands();
        game.getServer().getConsole().sendMessage(Text.of("SeriousVote has Loaded Successfully").toBuilder().color(TextColors.GOLD).build());
        game.getServer().getConsole().sendMessage(Text.of("Running Version 3.0 or something like that" ).toBuilder().color(TextColors.GREEN).build());

        if(!(databaseHostname=="" || databaseHostname == null)){
            milestones = new Milestones();
        } else {
            milestones = null;
        }



    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event)

    {
        Scheduler scheduler = Sponge.getScheduler();
        Task.Builder taskBuilder = scheduler.createTaskBuilder();
        Task task = taskBuilder.execute(new ExecuteCommands())
                .interval(1000, TimeUnit.MILLISECONDS)
                .name("SeriousVote-CommandRewardExecutor")
                .submit(plugin);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////COMMAND MANAGER//////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void registerCommands(){

        //////////////////////COMMAND BUILDERS///////////////////////////////////////////////
        CommandSpec reload = CommandSpec.builder()
                .description(Text.of("Reload your configs for seriousvote"))
                .permission("seriousvote.commands.admin.reload")
                .executor(new SVoteReload())
                .build();
        CommandSpec vote = CommandSpec.builder()
                .description(Text.of("Checks to see if it's running"))
                .permission("seriousvote.commands.vote")
                .executor(new SVoteVote())
                .build();

        CommandSpec giveVote = CommandSpec.builder()
                .description(Text.of("For admins to give a player a vote"))
                .permission("seriousvote.commands.admin.give")
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))))
                .executor(new SVoteGiveVote())
                .build();

        //////////////////////////COMMAND REGISTER////////////////////////////////////////////
        Sponge.getCommandManager().register(this, vote, "vote");
        Sponge.getCommandManager().register(this, reload,"svreload","seriousvotereload");
        Sponge.getCommandManager().register(this, giveVote, "givevote" );
    }

    //////////////////////////////COMMAND EXECUTOR CLASSES/////////////////////////////////////
    public class SVoteReload implements CommandExecutor {
        public CommandResult execute(CommandSource src, CommandContext args) throws
                CommandException {
            if (reloadConfigs()) {
                src.sendMessage(Text.of("Reloaded successfully!"));
            } else {
                src.sendMessage(Text.of("Could not reload properly :( did you break your config?"));
            }
            return CommandResult.success();
        }
    }

    public class SVoteGiveVote implements CommandExecutor {
        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

            Player player = args.<Player>getOne("player").get();

            player.sendMessage(Text.of("An administrator has awarded you a vote!"));
            giveVote(player.getName());
            currentRewards = "";
            src.sendMessage(Text.of("You have successfully given " + player.getName() + " a vote"));


            return CommandResult.success();
        }
    }

    public class SVoteVote implements CommandExecutor {
        public CommandResult execute(CommandSource src, CommandContext args) throws
                CommandException {
            src.sendMessage(Text.of("Thank You! Below are the places you can vote!").toBuilder().color(TextColors.GOLD).build());
            getVoteSites(rootNode).forEach(site -> {
                src.sendMessage(convertLink(site));
            });
            return CommandResult.success();



        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////CONFIGURATION METHODS//////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////
    public boolean reloadConfigs(){
        //try loading from file
        try {
            rootNode = loader.load();
        } catch (IOException e) {
            U.error("There was an error while reloading your configs");
            U.error(e.toString());
            return false;
        }

        //update variables and other instantiations
        publicMessage = getPublicMessage(rootNode);
        randomRewardsNumber = getRewardsNumber(rootNode);

        updateLoot(getRandomCommands(rootNode));
        buildChanceMap();
        setCommands = getSetCommands(rootNode);
        U.debug("Here's your commands");
        for(String ix : getRandomCommands(rootNode)){
            U.debug(ix);
        }


        //Load Offline votes
        U.info("Trying to load offline player votes from ... " + offlineVotes.toString());
        try {
            loadOffline();
        } catch (IOException e) {
            U.error("ahahahahaha We Couldn't load up the stored offline player votes",e);
        } catch (ClassNotFoundException e) {
            U.error("Well crap that is noooot a hash map! GO slap the dev!");
        }

        //Reload DB configuration
        databaseHostname = getDatabaseHostname(rootNode);
        databaseName = getDatabaseName(rootNode);
        databasePassword = getDatabasePassword(rootNode);
        databasePrefix = getDatabasePrefix(rootNode);
        databaseUsername = getDatabaseUsername(rootNode);
        databasePort = getDatabasePort(rootNode);

        if (milestones != null){
            milestones.reloadDB();

        }
        /////////Load Up Milestones/////////
        monthlySet = getMonthlySetCommands(rootNode);
        yearlySet = getYearlySetCommands(rootNode);
        weeklySet = getWeeklySetCommands(rootNode);


        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    private List<String> getWeeklySetCommands(ConfigurationNode node){
        return node.getNode("config","milestones","weekly","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    private List<String> getMonthlySetCommands(ConfigurationNode node){
        return node.getNode("config","milestones","monthly","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    private List<String> getYearlySetCommands(ConfigurationNode node){
        return node.getNode("config","milestones","yearly","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    ///////////////////////////////////////////////////////////////////////////////////////////
    private String getDatabaseName(ConfigurationNode node){
        return node.getNode("config","database","name").getString();
    }
    private String getDatabaseHostname(ConfigurationNode node){
        return node.getNode("config","database","hostname").getString();
    }
    private String getDatabasePort(ConfigurationNode node){
        return node.getNode("config","database","port").getString();
    }
    private String getDatabasePrefix(ConfigurationNode node){
        return node.getNode("config","database","prefix").getString();
    }
    private String getDatabaseUsername(ConfigurationNode node){
        return node.getNode("config","database","username").getString();
    }
    private String getDatabasePassword(ConfigurationNode node){
        return node.getNode("config","database","password").getString();
    }










    ////////////////////////////////////////////////////////////////////////////////////////////


    private List<String> getSetCommands(ConfigurationNode node) {
        return node.getNode("config","Rewards","set").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    private List<String> getRandomCommands(ConfigurationNode node) {
           return node.getNode("config","Rewards","random").getChildrenList().stream()
                   .map(ConfigurationNode::getString).collect(Collectors.toList());
    }
    private List<String> getVoteSites(ConfigurationNode node) {
        //TODO code potentially breaking here -- investigate
        return node.getNode("config","vote-sites").getChildrenList().stream()
                .map(ConfigurationNode::getString).collect(Collectors.toList());
    }

    //Returns the string value from the Config for the public message. This must be deserialized
    private String getPublicMessage(ConfigurationNode node){
        return node.getNode("config","broadcast-message").getString();
    }
    private int getRewardsNumber(ConfigurationNode node){
         int number = node.getNode("config", "random-rewards-number").getInt();
         isNoRandom = number == 0? true:false;
         rewardsMin = node.getNode("config", "rewards-min").getInt();
         rewardsMax = node.getNode("config", "rewards-max").getInt() + 1;
        return number;
    }

    public int generateRandomRewardNumber(){
        int nextInt;
        if(randomRewardsNumber < 0 && hasLoot) {
            //Inclusive
            if(rewardsMin < 0) rewardsMin = 0;
            if (rewardsMax > rewardsMin){
                nextInt =  ThreadLocalRandom.current().nextInt(rewardsMin,rewardsMax);
            } else {
                nextInt = 0;
                U.warn("There seems to be an error in your min/max setting in your configs.");
            }

            U.info("Giving out " + nextInt + " random rewards.");
            return nextInt;
        } else if(randomRewardsNumber < 0){
            return 0;
        }
        return 0;
    }

    public void updateLoot(List<String> lootTable){

        String[] inputLootTable = lootTable.stream().toArray(String[]::new);
        lootMap = new LinkedHashMap<Integer, List<Map<String,String>>>();
        chanceMap = new ArrayList<Integer>();
        //count to get the correct size of the lootMap
        for (int i = 0; i < inputLootTable.length; i+=3)
        {
            //get the current integer add it to the table, Since it is a Map duplicates will be removed
            lootMap.put(Integer.parseInt(inputLootTable[i]), new ArrayList<Map<String,String>>());
        }

        for (int i = 0; i < inputLootTable.length; i+=3)
        {
            //add in all the commands
            List lootList = lootMap.get(Integer.parseInt(inputLootTable[i]));
            Map<String,String>  lootEntry = new LinkedHashMap<String,String>();
            lootEntry.put(inputLootTable[i+1],inputLootTable[i+2]);
            lootList.add(lootEntry);
        }


        if (lootMap.size() == 0) {
            U.error("The lootMap Hasn't been loaded Check your config for errors!");
            hasLoot = false;
            return;        }
        hasLoot = true;
        U.info("Rewards for seriousVote Have been loaded successfully");


    }
    void buildChanceMap() {

        if (!hasLoot) {
            U.error("The lootMap Hasn't been loaded Check your config for errors!");
            return;
        } else {
            U.info("There are currently " + lootMap.size() + " Loot Tables");
            for (int i = 0; i < lootMap.size(); i++) {
                Map.Entry currentSet = Iterables.get(lootMap.entrySet(), i);
                Integer currentKey = Integer.parseInt(currentSet.getKey().toString());
                U.info("Gathering Table " + i + " of type" + currentKey);

                for (int ix = 0; ix < currentKey.intValue(); ix++) {
                    chanceMap.add(currentKey.intValue());
                }

            }
        }
    }



    ///////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////LISTENERS///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////
    @Listener
    public void onVote(VotifierEvent event)
    {
        Vote vote = event.getVote();
        String username = vote.getUsername();
        U.info("Vote Registered From " +vote.getServiceName() + " for "+ username);

        giveVote(username);

        if(isOnline(username)) {
            broadCastMessage(publicMessage, username);
        }


    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event){
        UUID playerID = event.getTargetEntity().getUniqueId();
        String username = event.getTargetEntity().getName();

        if(storedVotes.containsKey(playerID)){

            broadCastMessage(publicMessage, username);
            event.getTargetEntity().sendMessage(Text.of("Thanks for voting! Here are your rewards!").toBuilder().color(TextColors.AQUA).build());

            for(int ix = 0; ix < storedVotes.get(playerID).intValue(); ix ++){
                giveVote(username);
            }

            storedVotes.remove(playerID);
            try {
                saveOffline();
            } catch (IOException e) {
                U.error("Error while saving offline votes file", e);
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////ACTION METHODS///////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////
    private class ExecuteCommands implements Consumer<Task> {
        public void accept(Task task){

            for(String command:commandQueue)
            {
                game.getCommandManager().process(game.getServer().getConsole(),command );
            }
            commandQueue.clear();

        }
    }


    public void addCommands(List<String> commandList){
        for(String command:commandList){
            commandQueue.add(command);
        }

    }


    public boolean giveVote(String username){


        if (isOnline(username)) {
            currentRewards = "";
            if(hasLoot && !isNoRandom && randomRewardsNumber >= 1) {
                for (int i = 0; i < randomRewardsNumber; i++) {
                    U.info("Choosing a random reward.");
                    commandQueue.add(chooseReward(username));
                }
            } else if(hasLoot && !isNoRandom){
                randomRewardsGen = generateRandomRewardNumber();
                for (int i = 0; i < randomRewardsGen; i++) {
                    U.info("Choosing a random reward.");
                    commandQueue.add(chooseReward(username));
                }
            }
            //Get Set Rewards
            for(String setCommand: setCommands){
                commandQueue.add(parseVariables(setCommand, username, currentRewards));
            }

            if(!(milestones == null)){
                milestones.addVote(game.getServer().getPlayer(username).get().getUniqueId());
            }

        }
        else
        {
            U.info("Player was not online, saving vote for later use.");
            UUID playerID;

            if(userStorage.get().get(username).isPresent()){
                playerID = userStorage.get().get(username).get().getUniqueId();

                //Write to File
                if(storedVotes.containsKey(playerID)) {
                    storedVotes.put(playerID, storedVotes.get(playerID).intValue() + 1);
                } else {
                    storedVotes.put(playerID, new Integer(1));
                }
                try {
                    saveOffline();
                } catch (IOException e) {
                    U.error("Woah did that just happen? I couldn't save that offline player's vote!", e);
                }
            }



        }

        return true;
    }
    //Adds a reward(command) to the queue which is scheduled along with the main thread.
    //Bypass for Async NuVotifier
    public boolean queueReward(){

        return true;
    }
    public boolean broadCastMessage(String message, String username){
        if (message.isEmpty()) return false;
        game.getServer().getBroadcastChannel().send(
                TextSerializers.FORMATTING_CODE.deserialize(parseVariables(message, username, currentRewards)));
        return true;
    }
    public void gatherRandomRewards(){

    }
    //Chooses 1 random reward
    public String chooseReward(String username) {

        Integer reward = chanceMap.get(ThreadLocalRandom.current().nextInt(0, chanceMap.size()));
        U.info("Chose Reward from Table" + reward.toString());
        List<Map<String,String>> commandList = lootMap.get(reward);
        Map<String, String> commandMap = commandList.get(ThreadLocalRandom.current().nextInt(0, commandList.size()));
        Map.Entry runCommand = Iterables.get(commandMap.entrySet(),0);
        //Get "Name of reward"
        currentRewards += runCommand.getKey().toString() + " & ";
        return parseVariables(runCommand.getValue().toString(), username);

    }
    public Text convertLink(String link){
        Text textLink = TextSerializers.FORMATTING_CODE.deserialize(link);
        try {
            return textLink.toBuilder().onClick(TextActions.openUrl(new URL(textLink.toPlain()))).build();
        } catch (MalformedURLException e) {
            U.error("Malformed URL");
            U.error(e.toString());
        }
        return Text.of("Malformed URL - Inform Administrator");
    }
    public String parseVariables(String string, String username){
        return string.replace("{player}",username);
    }
    public String parseVariables(String string, String username, String currentRewards){
        if (isNoRandom){
            return parseVariables(string,username);
        } else if(currentRewards == "") {
            return string.replace("{player}",username).replace("{rewards}", "No Random Rewards");
        }
        return string.replace("{player}",username).replace("{rewards}", currentRewards.substring(0,currentRewards.length() -2));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////Utilities/////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////
    //returns weather a player is online
    private boolean isOnline(String username){
        if(getGame().getServer().getPlayer(username).isPresent()) return true;
        return false;
    }

    private void saveOffline() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(offlineVotes.toFile());
        ObjectOutputStream objectOutputStream= new ObjectOutputStream(fileOutputStream);

        objectOutputStream.writeObject(storedVotes);
        objectOutputStream.close();

    }
    private void loadOffline() throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream  = new FileInputStream(offlineVotes.toFile());
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

        storedVotes = (HashMap<UUID, Integer>) objectInputStream.readObject();
        objectInputStream.close();

    }

    public static SeriousVote getInstance(){
        return instance;
    }

    public static Optional<UserStorageService> getUserStorage(){
        return userStorage;
    }





}
