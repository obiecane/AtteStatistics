package sample;

import javax.swing.*;
import java.awt.*;

/**
 * @author Zhu Kaixiao
 * @version 1.0
 * @date 2019/9/20 10:01
 * @copyright 江西金磊科技发展有限公司 All rights reserved. Notice
 * 仅限于授权后使用，禁止非授权传阅以及私自用于商业目的。
 */
public class DDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private DDialog(JPanel panel) {
        super();
        init(panel);
        setModal(true);

    }

    private void init(JPanel panel) {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(panel);

        JPanel p = new JPanel();
        FlowLayout fl = new FlowLayout();
        fl.setHgap(15);
        p.setLayout(fl);
        c.add(p, BorderLayout.SOUTH);
    }

    public static void showDialog(String title, Component relativeTo, JPanel panel) {
        showDialog(title, relativeTo, panel, 400, 300);
    }

    public static void showDialog(String title, Component relativeTo, JPanel panel, int width, int height) {
        DDialog d = new DDialog(panel);
        d.setLocationRelativeTo(relativeTo);
        d.setTitle(title);
        Rectangle bounds = relativeTo.getBounds();
        d.setBounds((int) ((bounds.getWidth() - width) / 2 + bounds.x), (int) ((bounds.getHeight() - height) / 2 + bounds.y), width, height);
        d.setVisible(true);
    }

}
