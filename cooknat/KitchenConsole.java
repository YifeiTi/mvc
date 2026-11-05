import java.util.List;
import java.util.Scanner;

// Part 3a: a second view (and controller) for the terminal, using the same model as the web page
public class KitchenConsole implements Runnable {
    private final KitchenModel model;
    private final Scanner in = new Scanner(System.in);

    public KitchenConsole(KitchenModel model) {
        this.model = model;
    }

    // Reads one command per line, e.g., "on oven" or "temp 400"
    public void run() {
        System.out.println("Commands: status, on <appliance>, off <appliance>, temp <value>");
        while (in.hasNextLine()) {
            String[] words = in.nextLine().trim().toLowerCase().split("\\s+");
            String argument = words.length > 1 ? words[1] : "";
            switch (words[0]) {
                case "status" -> printStatus();
                case "on" -> turn(argument, true);
                case "off" -> turn(argument, false);
                case "temp" -> setTemperature(argument);
                case "" -> { }
                default -> System.out.println("Unknown command. Try: status, on oven, off dishwasher, temp 400");
            }
        }
    }

    // Turns "oven", "dishwasher", or "fridge" on (on = true) or off (on = false)
    private void turn(String name, boolean on) {
        if (!List.of("oven", "dishwasher", "fridge").contains(name)) {
            System.out.println("Unknown appliance. Try: oven, dishwasher, or fridge");
            return;
        }
        if (isOn(name) == on) {
            System.out.println("The " + name + " is already " + (on ? "on" : "off"));
            return;
        }
        // TODO 3a.2: turn the appliance on or off with the model, then printLastNotification()
        // Turning off the fridge requires an additional confirmation
        if (name.equals("fridge") && on == false){
            if(!confirm("Are you sure you want to turn the fridge off? (y/n)")){
                System.out.println("Fridge not turned off");
            }
        }
        model.toggle(name);
        printLastNotification();
    }

    // e.g., "temp 400"
    private void setTemperature(String value) {
        // TODO 3a.3: set the oven temperature with the model (use number(value)),
        // print the error if there is one, otherwise printLastNotification()
        String error = model.setOvenTemperature(number(value));
        if(error != null){
            System.out.println(error);
        }
        else{
            printLastNotification();
        }
    }

    // NO NEED TO CHANGE ANYTHING BELOW

    private boolean isOn(String name) {
        if (name.equals("oven")) return model.isOvenOn();
        if (name.equals("dishwasher")) return model.isDishwasherOn();
        if (name.equals("fridge")) return model.isFridgeOn();
        return false;
    }

    // Asks a yes/no question, returns true if the answer is "y"
    private boolean confirm(String question) {
        System.out.println(question);
        return in.hasNextLine() && in.nextLine().trim().equalsIgnoreCase("y");
    }

    private int number(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void printStatus() {
        System.out.println("Oven:       " + (model.isOvenOn() ? "ON " : "OFF") + "  " + model.getOvenTemperature() + " F");
        System.out.println("Dishwasher: " + (model.isDishwasherOn() ? "ON " : "OFF") + "  " + model.getDishwasherCycle());
        System.out.println("Fridge:     " + (model.isFridgeOn() ? "ON " : "OFF") + "  " + model.getFridgeTemperature() + " F");
        printLastNotification();
    }

    private void printLastNotification() {
        List<String> notifications = model.getNotifications();
        if (!notifications.isEmpty()) System.out.println("> " + notifications.get(notifications.size() - 1));
    }
}
