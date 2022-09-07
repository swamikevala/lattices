import java.util.ArrayList;
import java.util.List;

public class LatticeTest {

	public static void main(String args[]) throws InterruptedException {
		
		Lattice lattice = new Lattice();
		int iterations = 25;
		for (int i=0; i<iterations; i++) {
			lattice.grow();
		}
		
		// remove all non-integers for clarity
		
		filterIntegers(lattice);
		lattice.draw();
	}
	
	private static Lattice filterIntegers(Lattice lattice) {
		List<Node> filterNodes = new ArrayList<Node>();
		for (Node n : lattice.getGraph().vertexSet()) {
			if (!n.getWeight().fractional().eq(Rational.ZERO)) {
				filterNodes.add(n);
			}
		}
		for (Node n : filterNodes) {
			lattice.getGraph().removeVertex(n);
		}
		return lattice;
	}
	
	private static double density(long n) {
		List<Integer> factors = primeFactors(n);
		long product = 1;
		for (Integer f : factors) {
			product *= (f + 1);
		}
		return (double)product/n;
	}
	
	public static int isprime(int n){
      for(int i = 2; i<=Math.sqrt(n); i++){
        if(n%i==0)
          return 0;
      }
      return 1;
   }

   public static List<Integer> primeFactors(long n){
      List<Integer> factors = new ArrayList<Integer>();
	   for(int i = 2; i<= n; i++){
          if(isprime(i)==1){
             long x = n;
             while(x%i==0){
            	factors.add(new Integer((int)i));
                x /= i;
             }
          }
       }
	   return factors;
   }

}


