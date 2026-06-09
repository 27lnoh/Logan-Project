import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class StockMarketApp extends JFrame {
    // Configuration
    private double balance = 500.0;
    private double stockPrice = 100.0;
    private int shares = 0;
    private boolean autoTrade = false;
    private boolean showMemos = true;
    private double buyLimit = 80.0, sellLimit = 120.0;
    private final double GOAL = 1000.0; // Win Condition
    
    // Data
    private ArrayList<Candle> candles = new ArrayList<>();
    private java.util.List<Double> tickBuffer = new ArrayList<>();
    private Random rand = new Random();

    // UI Components
    private JLabel statusLabel = new JLabel();
    private GraphPanel graphPanel = new GraphPanel();
    private JTextArea historyArea = new JTextArea();

    public StockMarketApp() {
        setTitle("Market Pro Simulator");
        setSize(1100, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.DARK_GRAY);

        // Sidebar for History
        historyArea.setEditable(false);
        historyArea.setBackground(Color.BLACK);
        historyArea.setForeground(Color.GREEN);
        JScrollPane scroll = new JScrollPane(historyArea);
        scroll.setPreferredSize(new Dimension(250, 600));

        // Layout
        statusLabel.setForeground(Color.WHITE);
        add(statusLabel, BorderLayout.NORTH);
        add(graphPanel, BorderLayout.CENTER);
        add(scroll, BorderLayout.EAST);

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);
        JButton buyBtn = new JButton("Buy");
        JButton sellBtn = new JButton("Sell");
        JButton restartBtn = new JButton("Restart");
        JCheckBox autoBox = new JCheckBox("Auto-Trade", false);
        JCheckBox memoBox = new JCheckBox("Show Memos", true);

        buyBtn.addActionListener(e -> { if(balance >= stockPrice) { balance -= stockPrice; shares++; log("Manual Buy: $" + String.format("%.2f", stockPrice)); } });
        sellBtn.addActionListener(e -> { if(shares > 0) { balance += stockPrice; shares--; log("Manual Sell: $" + String.format("%.2f", stockPrice)); } });
        restartBtn.addActionListener(e -> resetGame());
        autoBox.addActionListener(e -> autoTrade = autoBox.isSelected());
        memoBox.addActionListener(e -> { showMemos = memoBox.isSelected(); graphPanel.repaint(); });

        controlPanel.add(buyBtn); controlPanel.add(sellBtn); controlPanel.add(restartBtn);
        controlPanel.add(autoBox); controlPanel.add(memoBox);
        add(controlPanel, BorderLayout.SOUTH);

        Timer timer = new Timer(500, e -> {
            updateMarket();
            graphPanel.repaint();
        });
        timer.start();
    }

    private void log(String msg) {
        historyArea.append(msg + "\n");
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    private void resetGame() {
        balance = 500.0;
        stockPrice = 100.0;
        shares = 0;
        candles.clear();
        tickBuffer.clear();
        graphPanel.offsetX = 0;
        graphPanel.offsetY = 0;
        graphPanel.zoom = 1.0;
        historyArea.setText("");
        log("Game Reset.");
    }

    private void updateMarket() {
        // Goal Check
        if (balance >= GOAL) {
            JOptionPane.showMessageDialog(this, "Congratulations! You reached $" + GOAL + "! You Win!");
            resetGame();
            return;
        }

        // --- Updated: Random News Event (Increased to 2% chance) ---
        if (rand.nextDouble() < 0.02) {
            boolean isBullish = rand.nextBoolean();
            double jump = 20.0 + (rand.nextDouble() * 30.0);
            
            if (isBullish) {
                stockPrice += jump;
                log("!!! BREAKING NEWS: Market Rally! Price surging!");
            } else {
                stockPrice = Math.max(0.0, stockPrice - jump);
                log("!!! BREAKING NEWS: Panic Selling! Price crashing!");
            }
        }
        // ----------------------------------------

        // Reset on Crash
        if (stockPrice <= 0) {
            log("!!! CRITICAL: Market crashed to $0. Restarting...");
            JOptionPane.showMessageDialog(this, "The market crashed! The game is resetting.");
            resetGame();
            return;
        }

        double change = (rand.nextDouble() - 0.49) * 5;
        stockPrice = Math.max(1.0, stockPrice + change);
        tickBuffer.add(stockPrice);

        if (tickBuffer.size() >= 5) {
            double open = tickBuffer.get(0);
            double close = tickBuffer.get(tickBuffer.size()-1);
            double high = tickBuffer.stream().mapToDouble(v -> v).max().orElse(open);
            double low = tickBuffer.stream().mapToDouble(v -> v).min().orElse(open);
            candles.add(new Candle(open, close, high, low));
            tickBuffer.clear();
        }

        if (autoTrade) {
            if (stockPrice <= buyLimit && balance >= stockPrice) { 
                balance -= stockPrice; shares++; 
                log("AUTO BUY: $" + String.format("%.2f", stockPrice));
            }
            if (stockPrice >= sellLimit && shares > 0) { 
                balance += stockPrice; shares--; 
                log("AUTO SELL: $" + String.format("%.2f", stockPrice));
            }
        }
        statusLabel.setText(String.format(" Balance: $%.2f | Price: $%.2f | Shares: %d", balance, stockPrice, shares));
    }

    class Candle {
        double o, c, h, l;
        Candle(double o, double c, double h, double l) { this.o=o; this.c=c; this.h=h; this.l=l; }
    }

    class GraphPanel extends JPanel {
        double offsetX = 0, offsetY = 0;
        double zoom = 1.0;
        private int lastMouseX, lastMouseY;

        GraphPanel() {
            setBackground(new Color(30, 30, 30));
            MouseAdapter ml = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastMouseX = e.getX(); lastMouseY = e.getY();
                    if (showMemos) {
                        int bY = (int)((300 - buyLimit * 2) * zoom + offsetY);
                        int sY = (int)((300 - sellLimit * 2) * zoom + offsetY);
                        if (e.getX() < 100 && Math.abs(e.getY() - bY) < 20) {
                            String res = JOptionPane.showInputDialog("Set Buy Limit:", buyLimit);
                            if(res != null) buyLimit = Double.parseDouble(res);
                        } else if (e.getX() < 100 && Math.abs(e.getY() - sY) < 20) {
                            String res = JOptionPane.showInputDialog("Set Sell Limit:", sellLimit);
                            if(res != null) sellLimit = Double.parseDouble(res);
                        }
                    }
                }
                public void mouseDragged(MouseEvent e) {
                    offsetX += (e.getX() - lastMouseX);
                    offsetY += (e.getY() - lastMouseY);
                    if (offsetX > 20) offsetX = 20;
                    if (offsetY > 500) offsetY = 500;
                    lastMouseX = e.getX(); lastMouseY = e.getY();
                    repaint();
                }
                public void mouseWheelMoved(MouseWheelEvent e) {
                    zoom = Math.max(0.2, Math.min(zoom - e.getWheelRotation() * 0.1, 5.0));
                    repaint();
                }
            };
            addMouseListener(ml); addMouseMotionListener(ml); addMouseWheelListener(ml);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int spacing = (int)(50 * zoom);
            int startY = (int)(offsetY % spacing);
            if (startY > 0) startY -= spacing;

            g2.setColor(new Color(60, 60, 60));
            for (int y = startY; y < getHeight(); y += spacing) {
                g2.drawLine(0, y, getWidth(), y);
                double price = (300 - (y - offsetY) / zoom) / 2.0;
                g2.drawString(String.format("$%.0f", price), 5, y - 2);
            }

            int x = (int)(20 + offsetX);
            for (Candle candle : candles) {
                g2.setColor(candle.c >= candle.o ? Color.GREEN : Color.RED);
                int h = (int)(Math.abs(candle.c - candle.o) * 2 * zoom);
                int y = (int)((300 - Math.max(candle.o, candle.c) * 2) * zoom + offsetY);
                if (x > -20 && x < getWidth()) g2.fillRect(x, y, (int)(10 * zoom), Math.max(h, 2));
                x += (int)(20 * zoom);
            }

            if (showMemos) {
                g2.setColor(Color.YELLOW);
                int bY = (int)((300 - buyLimit * 2) * zoom + offsetY);
                g2.drawLine(0, bY, getWidth(), bY);
                g2.fillRect(10, bY - 15, 80, 20);
                g2.setColor(Color.BLACK); g2.drawString("Buy: " + buyLimit, 15, bY);
                
                g2.setColor(Color.CYAN);
                int sY = (int)((300 - sellLimit * 2) * zoom + offsetY);
                g2.drawLine(0, sY, getWidth(), sY);
                g2.fillRect(10, sY - 15, 80, 20);
                g2.setColor(Color.BLACK); g2.drawString("Sell: " + sellLimit, 15, sY);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StockMarketApp().setVisible(true));
    }
}