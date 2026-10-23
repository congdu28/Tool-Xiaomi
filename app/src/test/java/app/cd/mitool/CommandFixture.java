package app.cd.mitool;

public class CommandFixture {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("timeout")) { Thread.sleep(10000); return; }
        for (int i = 0; i < 100000; i++) { System.out.print('o'); System.err.print('e'); }
        System.exit(7);
    }
}
