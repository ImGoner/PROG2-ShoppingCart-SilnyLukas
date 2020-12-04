package uhk.fim.gui;

import com.google.gson.Gson;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uhk.fim.IO.IOJsonFile;
import uhk.fim.IO.IOcsvFile;
import uhk.fim.model.ShoppingCart;
import uhk.fim.model.ShoppingCartItem;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class MainFrame extends JFrame implements ActionListener {
    JPanel panelMain;

    // Tlačítka deklarujeme zde, abychom k nim měli přístup v metodě actionPerformed
    JButton btnInputAdd;
    JTextField txtInputName, txtInputPricePerPiece;
    JSpinner spInputPieces;
    JCheckBox chckInputBought;
    JButton btnRemove;

    // Labels
    JLabel lblTotalPrice;
    JLabel lblTotalBought;
    JLabel lblDiff;

    //tabulka
    JTable table;


    JFileChooser fc;

    ShoppingCart shoppingCart;
    ShoppingCartTableModel shoppingCartTableModel;
    IOcsvFile iOCsv;
    IOJsonFile ioJson;

    public MainFrame(int width, int height) {
        super("PRO2 - Shopping cart");
        setSize(width, height);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initFC();
        iOCsv = new IOcsvFile();
        ioJson= new IOJsonFile();

        // Vytvoříme košík (data)
        shoppingCart = new ShoppingCart();
        // Vytvoříme model
        shoppingCartTableModel = new ShoppingCartTableModel();
        // Propojíme model s košíkem (data)
        shoppingCartTableModel.setShoppingCart(shoppingCart);

        initGUI();

        shoppingCart =  iOCsv.load("src/main/java/storage.csv");
        shoppingCartTableModel.fireTableDataChanged();

        refresh();
        updateFooter();
    }

    public void initGUI() {

        panelMain = new JPanel(new BorderLayout());

        // Menu
        createMenuBar();

        JPanel panelInputs = new JPanel(new FlowLayout(FlowLayout.LEFT)); // FlowLayout LEFT - komponenty chceme zarovnat zleva doprava.
        JPanel panelTable = new JPanel(new BorderLayout());
        JPanel panelFooter = new JPanel(new BorderLayout());

        // *** Formulář pro přidání položky ***
        // Název
        JLabel lblInputName = new JLabel("Název: ");
        txtInputName = new JTextField("", 15);
        // Cena za 1 kus
        JLabel lblInputPricePerPiece = new JLabel("Cena 1 kus: ");
        txtInputPricePerPiece = new JTextField("", 5);
        // Počet kusů
        JLabel lblInputPieces = new JLabel("Počet kusů: ");
        spInputPieces = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));

        JLabel lblInputBought = new JLabel("Cena 1 kus: ");
        chckInputBought = new JCheckBox("Koupeno");

        // Tlačítka
        btnInputAdd = new JButton("Přidat");
        btnInputAdd.addActionListener(this); // Nastavení ActionListeneru - kdo obslouží kliknutí na tlačítko.

        // Přidání komponent do horního panelu pro formulář na přidání položky
        panelInputs.add(lblInputName);
        panelInputs.add(txtInputName);
        panelInputs.add(lblInputPricePerPiece);
        panelInputs.add(txtInputPricePerPiece);
        panelInputs.add(lblInputPieces);
        panelInputs.add(spInputPieces);

        panelInputs.add(lblInputBought);
        panelInputs.add(chckInputBought);

        panelInputs.add(btnInputAdd);

        // *** Patička ***
        lblTotalPrice = new JLabel("");
        panelFooter.add(lblTotalPrice, BorderLayout.NORTH);

        lblTotalBought = new JLabel("");
        panelFooter.add(lblTotalBought,BorderLayout.WEST );

        lblDiff = new JLabel("");
        panelFooter.add(lblDiff, BorderLayout.SOUTH);




        // *** Tabulka ***
        table = new JTable();


        // Tabulku propojíme s naším modelem
        table.setModel(shoppingCartTableModel);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // Tabulku přidáme do panelu a obalíme ji komponentou JScrollPane
        panelTable.add(new JScrollPane(table), BorderLayout.CENTER);

        btnRemove = new JButton("Odeber vybraný řádek");
        btnRemove.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if (YNCDialog("Řádek bude nenávratně smazán =)","Remove row")==0){
                    shoppingCart.repoveItem(table.getSelectedRow());
                    refresh();
                }
            }
        });
        panelFooter.add(btnRemove,BorderLayout.EAST);

        // Přidání (pod)panelů do panelu hlavního
        panelMain.add(panelInputs, BorderLayout.NORTH);
        panelMain.add(panelTable, BorderLayout.CENTER);
        panelMain.add(panelFooter, BorderLayout.SOUTH);

        // Přidání hlavního panelu do MainFrame (JFrame)
        add(panelMain);


        refresh();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Soubor");
        fileMenu.add(new AbstractAction("Nový nákupní seznam") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (YNCDialog("Vaše data nenávratně zmizí v propadlišti dějin! Přejete si pokračovat?","Nový ShoppingCrart") == 0){
                shoppingCart = new ShoppingCart();
                refresh();
                }
            }
        });
        fileMenu.add(new AbstractAction("Uložit") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                iOCsv.save(shoppingCart,"src/main/java/storage.csv");
                JOptionPane.showMessageDialog(null, "Saved", "Success!", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        fileMenu.add(new AbstractAction("Uložit jako") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                saveFile();
            }
        });
        fileMenu.add(new AbstractAction("Načti Json/csv") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //loadJson();
                loadFromFile();
            }
        });
        fileMenu.setIcon(new ImageIcon("src/main/java/uhk/fim/source/folder.png"));
        menuBar.add(fileMenu);

        JMenu aboutMenu = new JMenu("O programu");
        aboutMenu.setIcon(new ImageIcon("src/main/java/uhk/fim/source/question.png"));
        menuBar.add(aboutMenu);

        setJMenuBar(menuBar);
    }

    // Při kliknutí na jakékoliv tlačítko se zavolá tato metoda.
    // Toho jsme docílili implementování rozhraní ActionListener a nastavením tlačítek např. btnInputAdd.addActionListener(this);
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        // Metoda se volá pro každé tlačítko, musíme tedy rozhodnout, co se má skutečně stát pro konkrétní tlačítka
        if (actionEvent.getSource() == btnInputAdd) {
            addProductToCart();
        }
    }

    private void addProductToCart() {
        if (!txtInputName.getText().isBlank()) {
            try {
                double price = Double.parseDouble(txtInputPricePerPiece.getText().replace(",", "."));
                if (price > 0) {
                    // Vytvořit novou položku
                    ShoppingCartItem item = new ShoppingCartItem(txtInputName.getText(), price, (int) spInputPieces.getValue(),chckInputBought.isSelected());
                    // Přidat položku do košíku
                    shoppingCart.addItem(item);
                    // Refreshnout tabulku
                    shoppingCartTableModel.fireTableDataChanged();
                    // Upravit patičku
                    updateFooter();
                } else {
                    JOptionPane.showMessageDialog(this, "Cena musí být větší než 0", "Chyba", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Zadejte správný formát ceny a počtu kusů!", "Chyba", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Došlo k chybě v programu!", "Chyba", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Vyplňte název produktu!", "Chyba", JOptionPane.ERROR_MESSAGE);
        }
        refresh();
    }

    private void updateFooter() {
        lblTotalPrice.setText("Celková cena: " + String.format("%.2f", shoppingCart.getTotalPrice()));
        lblTotalBought.setText("Celková cena zakoupených: " + String.format("%.2f", shoppingCart.getTotalBoughtPrice()));
        lblDiff.setText("Celková cena - zakoupené: " + String.format("%.2f", shoppingCart.getRozdil()));
    }


    private void loadFromFile(){


        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String fileName = fc.getSelectedFile().getAbsolutePath();
            if (fileName.endsWith("csv")){
                shoppingCart = iOCsv.load(fileName);
                shoppingCartTableModel.setShoppingCart(shoppingCart);
            }else if(fileName.endsWith("json")){
                shoppingCart = ioJson.load(fileName);
                shoppingCartTableModel.setShoppingCart(shoppingCart);
            }

        }
        refresh();
    }

    private void saveFile() {

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String fileName = fc.getSelectedFile().getAbsolutePath();
            if (!(fileName.endsWith(".csv")||fileName.endsWith(".json"))){JOptionPane.showMessageDialog(null,"Chybí koncovka(.json/.csv)","Error",JOptionPane.ERROR_MESSAGE);}
            if (fileName.endsWith(".csv")){
                iOCsv.save(shoppingCart,fileName);}
            else if(fileName.endsWith(".json")){
                ioJson.save(shoppingCart,fileName);
            }
        }
    }

    // Metoda, která načte xml - SAX
    // Mimo olivy můžete kouknout např. sem http://tutorials.jenkov.com/java-xml/sax-defaulthandler.html
    private void loadFileXmlSax() {
        try {
            // Char buffer, do kterého budeme zapisovat "hodnoty" elementů
            CharArrayWriter content = new CharArrayWriter();
            // Vytvoříme SAX parser
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            // Řekneme, který file chceme parsovat a vytvoříme handler, který obslouží události, které budou vznikat při parsování
            parser.parse(new File("src/uhk/fim/shoppingCart.xml"), new DefaultHandler() {
                // Parser narazil na otevřený tag
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    content.reset();
                }

                // Parser narazil na uzavřený tag
                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    System.out.println(qName + ": " + content.toString());
                }

                // Parser narazil na nějaký řetězec. Pozor, zavolá se i při nalezení odřádkování.
                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    super.characters(ch, start, length);
                    content.write(ch, start, length);
                }
            });

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metoda, která načte xml - DOM
    private void loadFileXmlDom() {
        try {
            // Vytvoříme builder
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            // Builder má metodu parse, která se postará o vytvoření objektu docoment, který má v sobě celou strukturu XML
            // Tím jsme si XML uložili do paměti a můžeme s ním dál pracovat = princim DOM
            Document document = builder.parse(new File("src/uhk/fim/shoppingCart.xml"));
            // Z dokumentu načteme prvního potomka = root = kořenová element
            Node root = document.getFirstChild();
            // Ukázka načtení typu nodu.
            short nodeType = root.getNodeType();
            // *** Zde je jen snaha ukázat, že je nutné strukturu projít nějakou rekurzí viz. ukázky v olivě ***
            // Má kořenový element potomky?
            if (root.hasChildNodes()) {
                // Ano má - načteme položky do seznamu
                NodeList list = root.getChildNodes();
                // Projdeme seznam
                for (int i = 0; i < list.getLength(); i++) {
                    // Načteme konkrétní node ze seznamu
                    Node nextNode = list.item(i);
                    // Opět se ptáme, jestli má potomky
                    if (nextNode.hasChildNodes()) {
                        // Ano má - načteme položky do seznamu
                        NodeList list2 = nextNode.getChildNodes();
                        // Projdeme seznam
                        for (int j = 0; j <= list2.getLength(); j++) {
                            // Načteme konkrétní node ze seznamu
                            Node nextNode2 = list2.item(j);
                            /// !!! Tady už můžete vidět, že je ntuné vytvořit nějakou rekurzi !!!
                            // Nemůžeme strukturu takto ručně projít.
                        }
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFileXmlDom4j() {
        DocumentFactory df = DocumentFactory.getInstance();
        SAXReader reader = new SAXReader(df);
        try {
            org.dom4j.Document doc = reader.read(new File("src/shoppingCart.xml"));
            System.out.println(doc.asXML());
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }


    private void refresh(){
        shoppingCartTableModel.setShoppingCart(shoppingCart);
        shoppingCartTableModel.fireTableDataChanged();
        updateFooter();
    }

    private void loadJson() {
        Gson gson = new Gson();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Simulace dlouhé odpovědi ze serveru
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    ShoppingCart cart = gson.fromJson(new InputStreamReader(
                            new URL("https://lide.uhk.cz/fim/student/benesja4/shoppingCart.json").openStream()
                    ), ShoppingCart.class);
                    System.out.println("Done");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

    }
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }

    private  void initFC(){
        fc = new JFileChooser();

        FileFilter ff = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {return true;}
                if (getFileExtension(file).equals("json") ||getFileExtension(file).equals("csv")) {return true;}else{return false;}
            }
            @Override
            public String getDescription(){

                return "json/csv";
            }
        };

        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(ff);
    }

    class CellButtonRebder extends JPanel implements TableCellRenderer {
        public Component getTableCellRendererComponent(
                final JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            this.add( new Button());
            return this;
        }
    }

    private int YNCDialog(String message, String title){
        return JOptionPane.showConfirmDialog(null,message,title, JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
    }
}