/*** line 7: symbol "out" is not a member of class "System"
 * COMP 520
 * Identification
 */
class PA4Test {
	
    public static void main(String[] args) {
        /* 1: simple literal */
        int x = 1;
    
        
        /* 9: array creation and length */
        int [] aa = new int [4];
        x = aa.length;
        System.out.println(2*x + 1);
        
        /* 10: array reference and update */
        aa[0] = 0;
        int i = 1;
        while (i < aa.length) {
            aa[i] = aa[i-1] + i;
            i = i + 1;
        }
        x = aa[3] + 4;
        System.out.println(x);
    }
}

class A
{
    int n;
    B b;
    
}

class B
{
    int n;
    A a;
    
    public int fact(int nn){
        int r = 1;
        if (nn > 1)
            r = nn * fact(nn -1);
        return r;
    }
}