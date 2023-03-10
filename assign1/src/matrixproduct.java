import java.util.*;
import static java.lang.Math.min;
import static java.lang.Math.pow;

public class matrixproduct {

    public void onMult(int m_ar, int m_br) {

        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];
        double temp;
        long startTime, endTime;
        double executionTime;

        for(int i = 0; i < m_ar; i++)
            for(int j = 0; j < m_ar; j++)
                pha[i*m_ar + j] = 1.0;

        for(int i = 0; i < m_br; i++)
            for(int j = 0; j < m_br; j++)
                phb[i*m_br + j] = i + 1;

        startTime = System.currentTimeMillis();

        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_br; j++) {
                temp = 0;
                for (int k = 0; k < m_ar; k++) {
                    temp += pha[i * m_ar + k] * phb[k * m_br + j];
                }
                phc[i * m_ar + j] = temp;
            }
        }

        endTime = System.currentTimeMillis();

        executionTime = (endTime - startTime) * 0.001;

        System.out.println("Time: " + executionTime + " seconds");
        System.out.println("Result matrix: ");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }

    // line by line matrix multiplication
    public void OnMultLine(int m_ar, int m_br) {

        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];
        long startTime, endTime;
        double executionTime;

        for(int i = 0; i < m_ar; i++)
            for(int j = 0; j < m_ar; j++)
                pha[i*m_ar + j] = 1.0;

        for(int i = 0; i < m_br; i++)
            for(int j = 0; j < m_br; j++)
                phb[i*m_br + j] = i + 1;

        startTime = System.currentTimeMillis();

        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_ar; j++) {
                for (int k = 0; k < m_br; k++) {
                    phc[i * m_ar + k] += pha[i * m_ar + j] * phb[j * m_br + k];
                }
            }
        }

        endTime = System.currentTimeMillis();

        executionTime = (endTime - startTime) * 0.001;

        System.out.println("Time: " + executionTime + " seconds");
        System.out.println("Result matrix: ");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }

    public static void main(String[] args) {

        int mode, lin, col;
        Scanner scanner = new Scanner(System.in);

        System.out.println("1. Multiplication");
        System.out.println("2. Line Multiplication");

        // Check if input is an integer and within the range of available modes
        do {
            System.out.print("Selection?: ");
            while (!scanner.hasNextInt()) {
                System.out.println("That's not an option!");
                System.out.print("Enter mode number (1-2): ");
                scanner.next();
            }
            mode = scanner.nextInt();
        } while (mode < 1 || mode > 2);

        System.out.print("Dimensions: lins=cols ? ");

        // Check if input is an integer and positive
        do {
            while (!scanner.hasNextInt()) {
                System.out.println("That's not a number!");
                System.out.print("Dimensions: lins=cols ? ");
                scanner.next();
            }
            lin = scanner.nextInt();
        } while (lin <= 0);

        col = lin;
        matrixproduct mp = new matrixproduct();

        switch (mode) {
            case 1 -> {
                scanner.close();
                mp.onMult(lin, col);
            }
            case 2 -> {
                scanner.close();
                mp.OnMultLine(lin, col);
            }
        }
    }
}
