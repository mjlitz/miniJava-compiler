       LOADL        0
       CALL         newarr  
       CALL         L10
       HALT   (0)   
L10:   PUSH         1
       LOADL        1
       STORE        3($sp)
       PUSH         1
       LOADL        4
       CALL         newarr  
       STORE        4($sp)
       LOAD         3[LB]
       LOAD         4[LB]
       CALL         arraylen
       STORE        3[LB]
       LOADL        2
       LOAD         3[LB]
       CALL         mult    
       LOADL        1
       CALL         add     
       CALL         putintnl
       PUSH         1
       LOADL        1
       STORE        5[LB]
       LOAD         5[LB]
       LOAD         4[LB]
       CALL         arraylen
       CALL         lt      
       JUMPIF (0)   L12
L11:   LOAD         5[LB]
       LOAD         5[LB]
       LOADL        1
       CALL         add     
       STORE        5[LB]
       LOAD         5[LB]
       LOAD         4[LB]
       CALL         arraylen
       CALL         lt      
       JUMPIF (1)   L11
L12:   LOAD         3[LB]
       LOADL        4
       CALL         add     
       STORE        3[LB]
       LOAD         3[LB]
       CALL         putintnl
