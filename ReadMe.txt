
1-	Install Ptolemy II,
2-	Go to the directory of your installation, then ptolemy/domains,
3-	Replace the atc folder in ptolemy/domains with our atc folder,
4-	Note that we have changed several parameters of the DE director from private/protected to public. 
	Therefore, replace the DEDirector.java with the previous ones,
5-	Invoke the vergil,
6-	After coming vergil up, go to the atc/demo/FirstPolicy and select a model. 


To run a model and generate its state space using the standard semantics of timed actors:

	open NReducedModel-10-10.xml for a 10*10 mesh structure
	OR
	open NReducedModel-18-18.xml for a 18*18 mesh structure
	
To run a model and generate its state space using the compositional method for POR of timed actors:
	
	open ReducedModel-10-10.xml for a 10*10 mesh structure
	OR
	open ReducedModel-18_18.xml for a 18*18 mesh structure


An input for a model with 10*10 mesh structure is placed in the input file-10-10 folder.
An input for a model with 18*18 mesh structure is placed in the input file-18-18 folder.

Place an input in the ptII directory and run the model. The number of states are written in outputAS1.txt.
Note that we executed the code using an Ubuntu machine. To calculate the execution time in Ubuntu we used "time" shell command.

Do not hesitate to contact maryam.bagheri1989@gmail.com if you have any problem.

Good Luck,
Maryam Bagheri.




