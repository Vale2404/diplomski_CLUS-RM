package speed.tests;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import redescriptionmining.Jacard;
import redescriptionmining.Rule;
import gnu.trove.set.hash.TIntHashSet;

public class JaccardTest {

	private static int maxNumber = 1000;
	private static BufferedWriter writer;
	private static Random rand = new Random();
	private static int maxSize = 10000;
	private static int brojUspjeha = 0;
	
	// 10 puta ili više - cijeli algoritam
	
	public JaccardTest() {
	    try {
			writer = new BufferedWriter(new FileWriter("C:\\Users\\anton\\OneDrive\\Desktop\\test1.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Rule[] createTestCases(TIntHashSet[] elements) {
		Rule r1 = new Rule();
		Rule r2 = new Rule();
		r1.elements = elements[0];
		r2.elements = elements[1];
		
		return new Rule[] { r1, r2 };
	}
	
	private TIntHashSet[] createElements(int size1, int size2) {		  
        TIntHashSet set1 = new TIntHashSet();
        TIntHashSet set2 = new TIntHashSet();
		
        for(int i = 0; i < size1; i++) {
        	set1.add(rand.nextInt(maxNumber));
        }
        
        for(int i = 0; i < size2; i++) {
        	set2.add(rand.nextInt(maxNumber));
        }
        
        return new TIntHashSet[] { set1, set2 };
	}
	
	private void testJaccard(int size1, int size2) throws IOException {
	   
		Jacard jaccard = new Jacard();
		Rule[] rules = createTestCases(createElements(size1, size2));
		String output = "size1: " + size1 + " size2: " + size2 + "\n";
		
		long reference = System.nanoTime();
		jaccard.computeJacard(rules[0], rules[1]);
		long finishm = System.nanoTime();

		double first = (double)(finishm-reference);
		output += first;
		
		jaccard = new Jacard();
		
		reference = System.nanoTime();
		jaccard.computeJacardSizeOptimization(rules[0], rules[1]);
		finishm = System.nanoTime();
		
		double second = (double)(finishm-reference);
		output += " " + second + " " + (first > second) + "\n";
		if(first > second) brojUspjeha++;
		writer.write(output);
	}
	
	public static void main(String[] args) throws IOException {
		JaccardTest  jaccardTest = new JaccardTest();
		int brojRunova = 10000;
		for(int i = 0; i < brojRunova; i++) {
			jaccardTest.testJaccard(10, 100);
		}
		System.out.println(((double)brojUspjeha / brojRunova) * 100 + "%");
		writer.close();
		
		Jacard jaccard = new Jacard();
		
		
		Rule[] rules = jaccardTest.createTestCases(jaccardTest.createElements(10, 15));
		long reference = System.nanoTime();
		for(int i = 0; i < brojRunova; i++) {
			jaccard.computeJacard(rules[0], rules[1]);
		}
		long finishm = System.nanoTime();

		reference = System.nanoTime();
		for(int i = 0; i < brojRunova; i++) {
			jaccard.computeJacardSizeOptimization(rules[0], rules[1]);
		}
		finishm = System.nanoTime();
	}
	
}
