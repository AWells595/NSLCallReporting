import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/*
*  Created by Alex Wells for NSL call monitoring
*  TODO create main menu to drive program through GUI
*  TODO create exporter to export as an excel sheet
*  TODO fix known error where anonymous missed calls crashes system
*  TODO record first and last call times
*  TODO record approximate daily call duration
*  TODO read monthly call stats and provide totals and averages for all stats
*
*/

public class Main {
    public static void main(String[] args) throws IOException {
        String date = pickDate();
        selectFile(date);
    }

    public static void selectFile(String date) throws IOException {
        JFileChooser fileChooser = new JFileChooser();

        String userDetails = System.getProperty("user.home");
        File downloadDirectory = new File(userDetails, "Downloads");
        fileChooser.setCurrentDirectory(downloadDirectory);

        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

        File file;
        fileChooser.setDialogTitle("Select Call List CSV");
        int result = fileChooser.showOpenDialog(null);
        if(result == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            showCallStats(file, date);
        }
    }

    private static String pickDate() {
        // Spinner with today's date
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(model);

        // Format as M/d/yyyy (e.g., 9/15/2025)
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "M/d/yyyy");
        dateSpinner.setEditor(editor);

        int option = JOptionPane.showOptionDialog(
                null,
                dateSpinner,
                "Select Date",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null
        );

        if (option == JOptionPane.OK_OPTION) {
            Date selected = (Date) dateSpinner.getValue();
            LocalDate localDate = selected.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.format(DateTimeFormatter.ofPattern("M/d/yyyy"));
        }

        return null;
    }

    public static void showMainMenu(){
        // not yet implemented
        JFrame mainMenu = new JFrame("NSL Call Volume Monitor");
    }

    public static void showCallStats(File file, String date) throws IOException {
        CallCounter counter = new CallCounter(file, date);
        counter.countCalls();
        JFrame frame = new JFrame("Call Stats");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(250, 300);
        frame.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 10, 10)); // one column, multiple rows

        panel.add(createCenteredLabel("         Total calls: " + counter.getTotalCalls()));
        panel.add(createCenteredLabel("       Inbound calls: " + counter.getInboundCalls()));
        panel.add(createCenteredLabel("      Outbound calls: " + counter.getRightPartyOutbound()));
        panel.add(createCenteredLabel("    Voice mails left: " + counter.getVoiceMails()));
        panel.add(createCenteredLabel("        Missed Calls: " + counter.getMissedCalls()));
        panel.add(createCenteredLabel("     Missed Calls Handled: " + counter.voiceMailsReturned()));
        panel.add(createCenteredLabel("     First Call time: " + counter.getFirstCallTime()));
        panel.add(createCenteredLabel("      Last Call time: " + counter.getLastCallTime()));

        frame.add(panel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JLabel createCenteredLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER); // center horizontally
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        return label;
    }
}