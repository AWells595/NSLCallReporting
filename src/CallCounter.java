import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class CallCounter {
    private final int INTERNAL_EXTENSION_LENGTH = 3;
    private final String INTERNAL_AREA_CODE = "(122)";
    private final String ALEX_CELLPHONE = "(603) 321-7375";

    private int inboundCalls;
    private int missedCalls;
    private int voiceMails;
    private int rightPartyOutbound;

    private ArrayList<String> missedPhoneNumbers;
    private ArrayList<String> answeredPhoneNumbers;
    private ArrayList<String> calledPhoneNumbers;
    private ArrayList<LocalTime> callTimes;

    private File file;
    private String targetDate;

    public CallCounter(String filePath, String targetDate) {
        this.inboundCalls = 0;
        this.voiceMails = 0;
        this.rightPartyOutbound = 0;
        this.file = new File(filePath);
        this.targetDate = targetDate;
    }

    public CallCounter(File csvFile, String targetDate) {
        this.file = csvFile;
        this.targetDate = targetDate;

        this.inboundCalls = 0;
        this.voiceMails = 0;
        this.rightPartyOutbound = 0;
        this.missedCalls = 0;

        missedPhoneNumbers = new ArrayList<>();
        answeredPhoneNumbers = new ArrayList<>();
        calledPhoneNumbers = new ArrayList<>();
        callTimes = new ArrayList<>();
    }

    public CallCounter(File filePath) {
        this.inboundCalls = 0;
        this.voiceMails = 0;
        this.rightPartyOutbound = 0;
        this.file = filePath;
        this.setTodayDate();
    }

    public CallCounter(String filePath) {
        this.inboundCalls = 0;
        this.voiceMails = 0;
        this.rightPartyOutbound = 0;
        this.file = new File(filePath);
        this.setTodayDate();
    }

    /**
     * Returns the total calls for a given day by adding inbound, voicemails, and right party outbounds calls
     * and returning the result.
     *
     **/
    public int getTotalCalls() {
        return this.inboundCalls + this.voiceMails + this.rightPartyOutbound;
    }

    /**
     * To match data from file date must be formatted as M/d/yyyy with no leading 0's
     *
     **/
    private void setTodayDate() {
        DateTimeFormatter formater = DateTimeFormatter.ofPattern("M/d/yyyy");
        this.targetDate = LocalDate.now().format(formater);
    }

    public int getMissedCalls() {
        return this.missedCalls;
    }

    public int getInboundCalls() {
        return this.inboundCalls;
    }

    public int getVoiceMails() {
        return this.voiceMails;
    }

    public int getRightPartyOutbound() {
        return rightPartyOutbound;
    }

    public LocalTime getFirstCallTime() {
        return Collections.min(callTimes);
    }

    public LocalTime getLastCallTime() {
        return Collections.max(callTimes);
    }

    public int voiceMailsReturned() {
        int returnedCalls = 0;
        for(String call:missedPhoneNumbers) {
            if (answeredPhoneNumbers.contains(call) || calledPhoneNumbers.contains(call)) {
                returnedCalls++;
            }
        }
        return returnedCalls;
    }

    private void addCallTime(String callTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        this.callTimes.add(LocalTime.parse(callTime.toUpperCase(), formatter));
    }

    /**
     * Determines if phone number is from an internal or external number.
     * Internal extensions are 3 digits long however sometimes the system picks up or dials the long form
     * number instead, all those numbers start with 122 as the area code which according to the North American
     * Numbering plan is not a valid area code for any phone number in the United States so it is safe to exclude
     * all numbers with that area code.
     *
     **/
    private boolean isExternal(String phoneNumber) {
        // anonymous callers don't have a number on the report sheet
        if (phoneNumber.isEmpty()) {
            return true;
        }
        // calls to and from self for testing purposes should not be counted
        if (phoneNumber.contains(ALEX_CELLPHONE)) {
            return false;
        }
        if (phoneNumber.length() > INTERNAL_EXTENSION_LENGTH) {
            return !phoneNumber.startsWith(INTERNAL_AREA_CODE);
        }
        return false;
    }

    private boolean isCorrectDate(String callDate) {
        return this.targetDate.equals(callDate);
    }

    /**
     * Helper function, takes callDuration from countCalls and determines if call is a voicemail or an outbound contact
     * then increments correct variable.
     *
     **/
    private void countOutboundCalls(String callDuration) {
        if (callDuration.endsWith("secs") || callDuration.endsWith("sec")) {
            // definitely under a minute
            voiceMails++;
        } else if (callDuration.endsWith("min") || callDuration.endsWith("mins")) {
            // parse the number before "min"
            String[] parts = callDuration.split(" ");
            int minutes = Integer.parseInt(parts[0]);
            if (minutes == 0) {
                this.voiceMails++; // e.g. "0 mins"
            } else {
                this.rightPartyOutbound++;
            }
        } else {
            // unused branch in theory but extra long calls may cause issues so defaults to rightPartyOutbound
            this.rightPartyOutbound++;
        }
    }

    /**
     * Parses CSV file to grab call data line by line.
     * For inbound calls increments inboundCall variable but for outbound calls relies on
     * countOutboundCalls() function passing the call duration to the helper function. Removes extraneous characters
     * from call data for easier processing.
     * <p>
     * Time complexity in all cases is O(N), needs to read the entire file line by line
     * but file will never be more than 301 lines long
     *
     **/
    public void countCalls() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            // data comes  in form ""data"" need to remove inner quotations
            line = line.replaceAll("\"", "");
            String[] call = line.split(",");
            boolean correctDate = isCorrectDate(call[0]);
            String callType = call[2].trim();

            if (correctDate) {
                if (callType.equals("Received")) {
                    if (isExternal(call[3])) {
                        this.inboundCalls++;
                        addCallTime(call[1]);
                        answeredPhoneNumbers.add(call[3]);
                    }
                }
                else if (callType.equals("Missed")) {
                    if (isExternal(call[3])) {
                        this.missedCalls++;
                        missedPhoneNumbers.add(call[3]);
                    }
                }
                else if (callType.equals("Dialed")){
                    String callDuration = call[call.length - 1].trim();
                    if (isExternal(call[7])) {
                        countOutboundCalls(callDuration);
                        addCallTime(call[1]);
                        calledPhoneNumbers.add(call[7]);
                    }
                }
            }
        }
    }
}
