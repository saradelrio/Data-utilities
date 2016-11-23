import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DataPartKfcv {
	private static String posClass;
	private static String negClass;
	
	public static void createPartFile (int k, String in, String [] out_tr, String [] out_tst) {
		File archivo = null;
		FileReader fr = null;
		BufferedReader br = null;
		String cabecera = leeCabeceraFichero(in);
		int posCount = 0;
		int negCount = 0;
		int totCount = 0;
		int currentPos = 0;
		int currentNeg = 0;
		int currentPartition = 0;
		int currentSample = 0;
		double posProbability;
		ArrayList <String> instances = new ArrayList <String> ();
		int [] posTest;
		int [] negTest;
		int [] posSamples;
		int [] negSamples;
		int [] selectedPosTest;
		int [] selectedNegTest;
		Random randomGenerator = new Random ();
		
		System.out.println("Reading Instances...");
		
		try {
			// Opening of the file and creation of BufferedReader to be able to make a comfortable reading (dispose of the method readLine ()).
			archivo = new File (in);
			fr = new FileReader (archivo);
			br = new BufferedReader(fr);
			
			// Reading the file
			String linea;
			String [] line;
			while((linea=br.readLine())!=null) {
				if (linea.indexOf("@") < 0) {
					instances.add(linea);
					line = linea.split(",");
					if (line[line.length-1].equalsIgnoreCase(posClass)) {
						posCount++;
					}
					else if (line[line.length-1].equalsIgnoreCase(negClass)) {
						negCount++;
					}
					else {
						System.err.println("Unknown class: " + linea);
						System.exit(-1);
					}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			// In the finally close the file, to ensure that both closes if all goes well and if an exception occurs.
			try {
				if( null != fr ) {
					fr.close();
				}
			}
			catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		System.out.println("posCount: "+posCount);
		System.out.println("negCount: "+negCount);
		
		if (posCount > negCount) {
			String tmp;
			int tmpCount;
			
			tmp = negClass;
			tmpCount = negCount;
			
			negCount = posCount;
			negClass = posClass;
			
			posCount = tmpCount;
			posClass = tmp;
		}
		
		totCount = posCount+negCount;
		
		// Initialize the list of examples belonging to each class
		posSamples = new int [posCount];
		negSamples = new int [negCount];
		currentPos = 0;
		currentNeg = 0;
		String [] instance;
		
		for (int i=0; i<instances.size(); i++) {
			// Check if positive or negative
			instance = instances.get(i).split(",");
			if (instance[instance.length-1].equalsIgnoreCase(posClass)) {
				posSamples[currentPos] = i;
				currentPos++;
			}
			else if (instance[instance.length-1].equalsIgnoreCase(negClass)) {
				negSamples[currentNeg] = i;
				currentNeg++;
			}			
		}
		
		System.out.println("Instancias leidas!");
		
		// Randomize in parts both classes		
		posTest = new int[k];
		negTest = new int[k];
		selectedPosTest = new int[k];
		selectedNegTest = new int[k];
		
		int sizePosTest = posCount/k;
		int sizeNegTest = negCount/k;
		
		// Los tamaños si no salen cuadrados, han de poder aumentar aleatoriamente en 1
		
		for (int i=0; i<k; i++) {
			posTest[i] = sizePosTest;
			negTest[i] = sizeNegTest;
			
			selectedPosTest[i] = 0;
			selectedNegTest[i] = 0;
		}
		
		ArrayList<Integer> listPos = null;
		if ((posCount%k) != 0) {
			int posAdditional = posCount%k;
			
			listPos = new ArrayList <Integer> (k);
			for (int i = 0; i < k; i++) {
				listPos.add(i);
			}
			
			while (posAdditional > 0) {
				int index = randomGenerator.nextInt(listPos.size());
				posTest[listPos.remove(index)]++;
				posAdditional--;
			}
		}
		
		if ((negCount%k) != 0) {
			int negAdditional = negCount%k;
			
			ArrayList<Integer> listNeg = new ArrayList <Integer> (k);
			ArrayList<Integer> list = new ArrayList <Integer> (k);
			
			while ((negAdditional > 0) &&  (listPos != null) && (listPos.size() > 0)) {
				int index = randomGenerator.nextInt(listPos.size());
				listNeg.add(listPos.get(index));
				negTest[listPos.remove(index)]++;
				negAdditional--;
			}
			
			if (negAdditional > 0) {
				for (int i = 0; i < k; i++) {
					if (!listNeg.contains(i)) {
						list.add(i);
					}
				}
				
				while (negAdditional > 0) {
					int index = randomGenerator.nextInt(list.size());
					negTest[list.remove(index)]++;
					negAdditional--;
				}
			}
		}
		
		System.out.println("Selecting partitions...");
		
		shuffleArray(posSamples, randomGenerator);
		shuffleArray(negSamples, randomGenerator);
		
		// Create new output files
		System.out.println("Creating the datasest");
		
		FileWriter [] fichero_tr = new FileWriter[k];
		FileWriter [] fichero_tst = new FileWriter[k];
		PrintWriter [] pw_tr = new PrintWriter[k];
		PrintWriter [] pw_tst = new PrintWriter[k];
		
		try {
			for (int i=0; i<k; i++) {
				fichero_tr[i] = new FileWriter(out_tr[i]);
				fichero_tst[i] = new FileWriter(out_tst[i]);
				
				pw_tr[i] = new PrintWriter(fichero_tr[i]);
				pw_tst[i] = new PrintWriter(fichero_tst[i]);
				
				pw_tst[i].print(cabecera);
				pw_tr[i].print(cabecera);
			}
			
			// Randomize instance presentation			
			currentPartition = 0;
			currentPos = 0;
			currentNeg = 0;
			posProbability = (double)posCount/(double)totCount;
			
			for (currentPartition=0; currentPartition<k; currentPartition++) {
				double classProb;
				
				// Examples from either class can be added
				while ((selectedPosTest[currentPartition] < posTest[currentPartition]) && (selectedNegTest[currentPartition] < negTest[currentPartition])) {
					classProb = randomGenerator.nextDouble();
					
					// Select the class of the sample
					if (classProb < posProbability) {
						currentSample = posSamples[currentPos+selectedPosTest[currentPartition]];
					}
					else {
						currentSample = negSamples[currentNeg+selectedNegTest[currentPartition]];
					}
					
					// Add to the test set
					pw_tst[currentPartition].println(instances.get(currentSample));
					
					// Add to the training sets
					for (int j=0; j<k; j++) {
						if (j != currentPartition) {
							pw_tr[j].println(instances.get(currentSample));
						}
					}
					
					if (classProb < posProbability) {
						selectedPosTest[currentPartition]++;
					}
					else {
						selectedNegTest[currentPartition]++;
					}
				}
				
				// One of the classes cannot add any more examples from its class. Adding examples until we fill both classes
				while (selectedPosTest[currentPartition] < posTest[currentPartition]) {
					currentSample = posSamples[currentPos+selectedPosTest[currentPartition]];
					
					// Add to the test set
					pw_tst[currentPartition].println(instances.get(currentSample));
					
					// Add to the training sets
					for (int j=0; j<k; j++) {
						if (j != currentPartition) {
							pw_tr[j].println(instances.get(currentSample));
						}
					}
					
					selectedPosTest[currentPartition]++;
				}
				
				while (selectedNegTest[currentPartition] < negTest[currentPartition]) {
					currentSample = negSamples[currentNeg+selectedNegTest[currentPartition]];
					
					// Add to the test set
					pw_tst[currentPartition].println(instances.get(currentSample));
					
					// Add to the training sets
					for (int j=0; j<k; j++) {
						if (j != currentPartition) {
							pw_tr[j].println(instances.get(currentSample));
						}
					}
					
					selectedNegTest[currentPartition]++;					
				}
				
				currentPos += posTest[currentPartition];
				currentNeg += negTest[currentPartition];				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {				
				for (int i=0; i<k; i++) {
					if (null != fichero_tr[i])
						fichero_tr[i].close();
					if (null != fichero_tst[i])
						fichero_tst[i].close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}
	
	public static void shuffleArray(int[] array, Random random) {
    int index;

    for (int i = array.length - 1; i > 0; i--)
    {
        index = random.nextInt(i + 1);
        if (index != i)
        {
            array[index] ^= array[i];
            array[i] ^= array[index];
            array[index] ^= array[i];
        }
    }
	}
	
	public static String leeCabeceraFichero (String filename) {
		File archivo = null;
		FileReader fr = null;
		BufferedReader br = null;
		ArrayList <String> attInfo = null;
		boolean end_cabecera;
		String cabecera = "";
		
		System.out.println("Leyendo cabecera...");
		
		try {
			archivo = new File (filename);
			fr = new FileReader (archivo);
			br = new BufferedReader(fr);
			attInfo = new ArrayList <String> ();
			String attClass = "";
			boolean found = false;
			
			// Reading the file
			String linea;
			end_cabecera = false;
			while (((linea=br.readLine())!=null) && !end_cabecera) {
				cabecera = cabecera + linea + "\n";
				
				if (linea.indexOf("@data") > -1) {
					end_cabecera = true;
				}
				else {
					if (linea.indexOf("attribute") > -1) {
						attInfo.add(linea);
					}
					else if (linea.indexOf("outputs") > -1) {
						attClass = linea.substring(linea.indexOf("outputs")+8,linea.length());
					}
				}
			}
			
			for (int i=0; i<attInfo.size() && !found; i++) {
				if (attInfo.get(i).indexOf(attClass) > -1) {
					String aux = attInfo.get(i);
					found = true;
					posClass = aux.substring(aux.indexOf("{")+1, aux.indexOf(",")).trim();
					negClass = aux.substring(aux.indexOf(",")+1, aux.indexOf("}")).trim();
				}
			}
		}
    catch(Exception e) {
			e.printStackTrace();
    }
    finally {
     	try {
     		if( null != fr ) {
     			fr.close();
     		}
     	}
     	catch (Exception e2) {
     		e2.printStackTrace();
			}
		}
		
		System.out.println("Identified header!");
		return cabecera;
	}
	
	public static void main (String[] args) {		
	
		if (args.length != 2) {
			System.err.println("The usage of this program is <DataPartKfcv K DATA_PART>");
			System.exit(-1);
		}
		
		int k;
		String input_dat;
		String [] output_tr_dat;
		String [] output_tst_dat;
		
		k = Integer.parseInt(args[0]);
		input_dat = args[1];
		output_tr_dat = new String [k];
		output_tst_dat = new String [k]; 
		
		for (int i=0; i<k; i++) {
			output_tr_dat[i] = input_dat.substring(0,input_dat.indexOf(".dat")) + "-" + k + "-" + (i+1) + "tra.dat";
			output_tst_dat[i] = input_dat.substring(0,input_dat.indexOf(".dat")) + "-" + k + "-" + (i+1) + "tst.dat";
		}
		
		createPartFile (k, input_dat, output_tr_dat, output_tst_dat);
	}
}

