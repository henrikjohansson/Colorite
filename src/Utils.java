import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class Utils extends JFrame {

	public static void errorWindow(int screenWidth, int screenHeight, int windowWidth, int windowHeight, String text) {
		final JFrame error = new JFrame("Colorite: Error");
		
		error.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        error.setSize(200, 100);
        error.setLocation((screenWidth - windowWidth)/2, (screenHeight - windowHeight)/2);
        error.setLayout(new FlowLayout());
        error.add(new JLabel(text));
        JButton ok = new JButton("Ok");
        ok.setBounds(50, 60, 80, 30);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                error.dispose();
           }
        });
        error.add(ok);
        error.setVisible(true);
	}
	
}
