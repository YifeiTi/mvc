import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class KitchenModel {
    public static final List<String> CYCLES = List.of("Normal", "Eco", "Heavy"); // used by the view

    // Note: Approach the TODOs in order (2.1 to 2.5, same numbers in KitchenController)
    // Make sure to test and run the app after each one!

    private boolean ovenOn = false;
    private int ovenTemperature = 350;
    private boolean dishwasherOn = false;
    private String dishwasherCycle = "Normal";
    private boolean fridgeOn = true;
    private int fridgeTemperature = 37;
    private final List<String> notifications = new ArrayList<>();
    private final Timer timer = new Timer(true);

    // Used by the view
    public synchronized boolean isOvenOn() { return ovenOn; }
    public synchronized int getOvenTemperature() { return ovenTemperature; }
    public synchronized boolean isDishwasherOn() { return dishwasherOn; }
    public synchronized String getDishwasherCycle() { return dishwasherCycle; }
    public synchronized boolean isFridgeOn() { return fridgeOn; }
    public synchronized int getFridgeTemperature() { return fridgeTemperature; }
    public synchronized List<String> getNotifications() { return new ArrayList<>(notifications); } // a copy

    // Turns "oven", "dishwasher", or "fridge" on or off, incl. your rule from Pt. 1 and the timers
    // Hint: one private method per appliance (e.g., toggleOven())
    public synchronized void toggle(String name) {
        if (name.equals("oven")){
            if (dishwasherOn){
                notifications.add("Oven cannot be turned on, the dishwasher is running");
            }
            else{
                ovenOn = !ovenOn;
                notifications.add("Oven turned " + (ovenOn ? "on" : "off"));
                if (ovenOn) {
                    later(1500, () -> notifications.add("Oven pre-heated to " + ovenTemperature + " F"));
                }
            }
        }
        else if (name.equals("dishwasher")){
            if (ovenOn){
                notifications.add("Dishwasher cannot be turned on, the oven is running");
             }
            else{
                dishwasherOn = !dishwasherOn;
                notifications.add("Dishwasher turned " + (dishwasherOn ? "on" : "off"));
                if (dishwasherOn) {
                    later(3000, ()-> {notifications.add("Dishwasher finished the " + dishwasherCycle +  " cycle");
                    dishwasherOn = false;});
                }
            }
        }
        else if(name.equals("fridge")){
            fridgeOn = !fridgeOn;
            notifications.add("Fridge turned " + (fridgeOn ? "on": "off"));
        }
        
    }


    // Returns an error message, or null if the temperature was set
    public synchronized String setOvenTemperature(int value) {
        if (value < 170 || value > 550 ){
            return "Please enter a temperature from 170 degrees to 550 degrees";
        }
        else{
            ovenTemperature = value;
            notifications.add("Oven set to" + value + "F");
            return null;
        }
    }

    // Returns an error message, or null if the cycle was set
    public synchronized String setDishwasherCycle(String cycle) {
        if (dishwasherOn){
            return "You cannot change the cycle while the dishwasher is running";
        }
        dishwasherCycle = cycle;
        notifications.add("Dishwasher cycle set to " + cycle);
        return null;
    }

    // Turns off everything except the fridge
    public synchronized void allOff() {
       if (ovenOn) {
            ovenOn = false;
            notifications.add("Oven turned off");
        }
        if (dishwasherOn) {
            dishwasherOn = false;
            notifications.add("Dishwasher turned off");
        }
    }

    // Runs an action after a delay (for the timers), e.g., later(1500, () -> notifications.add("..."));
    private void later(long millis, Runnable action) {
        timer.schedule(new TimerTask() {
            public void run() {
                synchronized (KitchenModel.this) {
                    action.run();
                }
            }
        }, millis);
    }
}
