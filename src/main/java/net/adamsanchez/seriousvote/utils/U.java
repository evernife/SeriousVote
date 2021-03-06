package net.adamsanchez.seriousvote.utils;

import net.adamsanchez.seriousvote.SeriousVote;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Created by adam_ on 01/22/17.
 */
public class U {
    public static void info(String info){
        SeriousVote.getInstance().getLogger().info(info + CC.RESET);
    }
    public static void debug(String debug){
        if(SeriousVote.getInstance().isDebug()) {
            SeriousVote.getInstance().getLogger().info(debug + CC.RESET);
        }
    }
    public static void error(String error) {
        SeriousVote.getInstance().getLogger().error(error + CC.RESET);
    }
    public static void error(String error, Exception e){
        SeriousVote.getInstance().getLogger().error(error + CC.RESET,e);
    }
    public static void warn(String warn){
        SeriousVote.getInstance().getLogger().warn(warn + CC.RESET);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getName(UUID player){
        Optional<UserStorageService> userStorage =  SeriousVote.getUserStorage();
        return userStorage.get().get(player).get().getName();
    }
    public static UUID getIdFromName(String name){
        Optional<UserStorageService> userStorage =  SeriousVote.getUserStorage();
        if((userStorage.get().get(name).isPresent())){
            return userStorage.get().get(name).get().getUniqueId();
        } else {
            return null;
        }
    }

    public static int roll(int chanceMax){
        //Returns a number within the chance pool bound is lower inclusive upper exclusive
        int nextInt;
        if (chanceMax > 0) {

            nextInt = ThreadLocalRandom.current().nextInt(0, chanceMax + 1);
            U.debug("Rolled a " + nextInt + " out of" + chanceMax + "for table.");
            return nextInt;
        }
        return 0;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void bcast(String msg, String username){
        SeriousVote.getInstance().broadCastMessage(msg,username);
    }


}
