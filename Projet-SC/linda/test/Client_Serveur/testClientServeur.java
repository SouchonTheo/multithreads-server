package linda.test;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import linda.Tuple;
import linda.server.LindaClient;

public class testClientServeur {

    static LindaClient client;
    public static TextArea text;
    public static TextField data;
    public static Frame frame;

    public static String subject = "MyQueue";

    public static Vector<String> users = new Vector<String>();
    public static String myName;

    public static void main(String argv[]) {

        myName = argv[0];
        String myURL = "//localhost:4000/LindaServer";
        client = new LindaClient(myURL);

        // creation of the GUI
        frame = new Frame(myName);
        frame.setLayout(new FlowLayout());

        data = new TextField(55);
        frame.add(data);

        Button write_button = new Button("write");
        write_button.addActionListener(new writeListener());
        frame.add(write_button);

        Button take_button = new Button("take");
        take_button.addActionListener(new takeListener());
        frame.add(take_button);

        Button read_button = new Button("read");
        read_button.addActionListener(new readListener());
        frame.add(read_button);

        Button tryread_button = new Button("tryread");
        tryread_button.addActionListener(new tryreadListener());
        frame.add(tryread_button);

        Button trytake_button = new Button("trytake");
        trytake_button.addActionListener(new trytakeListener());
        frame.add(trytake_button);

        frame.setSize(470, 300);
        frame.setVisible(true);
    }

}

// action invoked when the "write" button is clicked
class writeListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
        try {
            // https://www.delftstack.com/fr/howto/java/how-to-check-if-a-string-is-a-number-in-java/
            System.out.println("write button pressed");
            Tuple t2 = new Tuple("hello", 15);
            System.out.println("(2) write: " + t2);
            testClientServeur.client.write(t2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

// action invoked when the "take" button is clicked
class takeListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
        try {
            System.out.println("take button pressed");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

// action invoked when the "read" button is clicked
class readListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
        try {
            System.out.println("read button pressed");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

// action invoked when the "tryread" button is clicked
class tryreadListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
        try {
            System.out.println("tryread button pressed");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

// action invoked when the "trytake" button is clicked
class trytakeListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
        try {
            System.out.println("trytake button pressed");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
