module A(output Qa, Qb, input R, S);

   nor (Qa, R, Qb);
   nor (Qb, S, Qa);


endmodule
