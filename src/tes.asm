  0         LOADL        0
  1         CALL         newarr  
  2         CALL         L10
  3         HALT   (0)   
  4  L10:   PUSH         1
  5         LOADL        1
  6         STORE        3[LB]
  7         PUSH         1
  8         LOADL        4
  9         CALL         newarr  
 10         STORE        4[LB]
 11         LOAD         3[LB]
 12         LOAD         4[LB]
 13         CALL         arraylen
 14         STORE        3[LB]
 15         LOADL        2
 16         LOAD         3[LB]
 17         CALL         mult    
 18         LOADL        1
 19         CALL         add     
 20         CALL         putintnl
 21         PUSH         1
 22         LOADL        1
 23         STORE        5[LB]
 24         LOAD         5[LB]
 25         LOAD         4[LB]
 26         CALL         arraylen
 27         CALL         lt      
 28         JUMPIF (0)   L12
 29  L11:   LOAD         5[LB]
 30         LOAD         5[LB]
 31         LOADL        1
 32         CALL         add     
 33         STORE        5[LB]
 34         LOAD         5[LB]
 35         LOAD         4[LB]
 36         CALL         arraylen
 37         CALL         lt      
 38         JUMPIF (1)   L11
 39  L12:   LOAD         3[LB]
 40         LOADL        4
 41         CALL         add     
 42         STORE        3[LB]
 43         LOAD         3[LB]
 44         CALL         putintnl
