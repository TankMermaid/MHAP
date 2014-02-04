package com.secret.fastalign.utils;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Random;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.secret.fastalign.general.Sequence;

public final class Utils {

   public enum ToProtein
   {
      AAA("K"),
      AAC("N"),
      AAG("K"),
      AAT("N"),
      ACA("T"),
      ACC("T"),
      ACG("T"),
      ACT("T"),
      AGA("R"),
      AGC("S"),      
      AGG("R"),
      AGT("S"),
      ATA("I"),
      ATC("I"),
      ATG("M"),
      ATT("I"),
      CAA("Q"),
      CAC("H"),
      CAG("Q"),
      CAT("H"),
      CCA("P"),
      CCC("P"),
      CCG("P"),
      CCT("P"),
      CGA("R"),
      CGC("R"),
      CGG("R"),
      CGT("R"),
      CTA("L"),
      CTC("L"),
      CTG("L"),
      CTT("L"),
      GAA("E"),
      GAC("D"),
      GAG("E"),
      GAT("D"),
      GCA("A"),
      GCC("A"),
      GCG("A"),
      GCT("A"),
      GGA("G"),
      GGC("G"),
      GGG("G"),
      GGT("G"),
      GTA("V"),
      GTC("V"),
      GTG("V"),
      GTT("V"),
      TAA("X"),
      TAC("Y"),
      TAG("X"),
      TAT("Y"),
      TCA("S"),
      TCC("S"),
      TCG("S"),
      TCT("S"),
      TGA("X"),
      TGC("C"),
      TGG("W"),
      TGT("C"),
      TTA("L"),
      TTC("F"),
      TTG("L"),
      TTT("F");
      
      /*
      Ala/A    GCU, GCC, GCA, GCG   
      Leu/L    UUA, UUG, CUU, CUC, CUA, CUG
      Arg/R    CGU, CGC, CGA, CGG, AGA, AGG  
      Lys/K    AAA, AAG
      Asn/N    AAU, AAC    
      Met/M    AUG
      Asp/D    GAU, GAC    
      Phe/F    UUU, UUC
      Cys/C    UGU, UGC    
      Pro/P    CCU, CCC, CCA, CCG
      Gln/Q    CAA, CAG    
      Ser/S    UCU, UCC, UCA, UCG, AGU, AGC
      Glu/E    GAA, GAG    
      Thr/T    ACU, ACC, ACA, ACG
      Gly/G    GGU, GGC, GGA, GGG   
      Trp/W    UGG
      His/H    CAU, CAC    
      Tyr/Y    UAU, UAC
      Ile/I    AUU, AUC, AUA  
      Val/V    GUU, GUC, GUA, GUG
      START    AUG   
      STOP  UAG, UGA, UAA
      */
       private String other;

       ToProtein( String other )
       {
          this.other = other;
       }
       public String getProtein()
       {
          return this.other;
       }
   }
   public enum Translate
   {
       A("T"),
       C("G"),
       G("C"),
       N("N"),
       T("A");

       private String other;

       Translate( String other )
       {
          this.other = other;
       }
       public String getCompliment()
       {
          return this.other;
       }
   }
   
   public static final int FASTA_LINE_LENGTH = 60;
   
   public static MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
   
   public static final int MBYTES = 1048576;


   public static int checkForEnd(String line, int brackets) {
      if (line.startsWith("{")) {
         brackets++;
      }
      if (line.startsWith("}")) {
         brackets--;
      }
      if (brackets == 0) {
         return -1;
      }
      
      return brackets;
   }

   public final static long[][] computeKmerHashes(final Sequence seq, final int kmerSize, final int numWords)
	{
		if (numWords%2!=0)
			throw new FastAlignRuntimeException("Number of words must be a multiple of 2.");
	
		final int numberKmers = seq.numKmers(kmerSize);
		
		if (numberKmers<1)
			throw new FastAlignRuntimeException("Kmer size bigger than string length.");
	
		//might want to change to city hash if it comes out
		HashFunction hf = Hashing.murmur3_128(0);
		
		//get the rabin hashes
		final int[] rabinHashes = computeRabinHashes(seq, kmerSize);
	
		final long[][] hashes = new long[rabinHashes.length][numWords];
		
		for (int iter=0; iter<rabinHashes.length; iter++)
		{
			for (int word128=0; word128<numWords/2; word128++)
			{
				final Hasher hasher = hf.newHasher(0);
				final HashCode code = hasher.putInt(rabinHashes[iter]).putInt(word128).hash();
	
				//store the code
				LongBuffer bb = ByteBuffer.wrap(code.asBytes()).asLongBuffer();
				hashes[iter][word128*2+0] = bb.get(0);
				hashes[iter][word128*2+1] = bb.get(1);
			}
		}
		
		return hashes;
	}

   public final static int[][] computeKmerHashesInt(final Sequence seq, final int kmerSize, final int numWords)
	{
		final int numberKmers = seq.numKmers(kmerSize);
		
		if (numberKmers<1)
			throw new FastAlignRuntimeException("Kmer size bigger than string length.");
	
		//might want to change to city hash if it comes out
		HashFunction hf = Hashing.murmur3_32(0);
		
		//get the rabin hashes
		final int[] rabinHashes = computeRabinHashes(seq, kmerSize);
	
		final int[][] hashes = new int[rabinHashes.length][numWords];
		
		for (int iter=0; iter<rabinHashes.length; iter++)
		{
			for (int word=0; word<numWords; word++)
			{
				final Hasher hasher = hf.newHasher(0);
				hashes[iter][word] = hasher.putInt(rabinHashes[iter]).putInt(word).hash().asInt();
			}
		}
		
		return hashes;
	}
   
   public final static int[] computeRabinHashes(final Sequence seq, final int kmerSize)
	{
		//RabinKarpSeqHash rabinHash = new RabinKarpSeqHash(kmerSize);
		//final int[] rabinHashes = rabinHash.hashInt(seq.getString());
		
		HashFunction hf = Hashing.murmur3_32(0);
	
		final int[] rabinHashes = new int[seq.numKmers(kmerSize)];
		for (int iter=0; iter<seq.numKmers(kmerSize); iter++)
		{
			String kmer = seq.getKmer(iter, kmerSize);
			
			rabinHashes[iter] = hf.newHasher(0).putUnencodedChars(kmer).hash().asInt();
		}
		
		return rabinHashes;
	}

   // add new line breaks every FASTA_LINE_LENGTH characters
   public static String convertToFasta(String supplied) {
      StringBuffer converted = new StringBuffer();
      int i = 0;
      String[] split = supplied.trim().split("\\s+");
      if (split.length > 1) { //process as a qual
         int size = 0;
         for (i = 0; i < split.length; i++) {
            converted.append(split[i]);
            size+= split[i].length();
            if (i != (split.length - 1)) {
               if (size >= FASTA_LINE_LENGTH) {
                  size = 0;
                  converted.append("\n");
               } else {
                  converted.append(" ");
               }
            }
         }
      } else {
         for (i = 0; (i+FASTA_LINE_LENGTH) < supplied.length(); i+= FASTA_LINE_LENGTH) {
            converted.append(supplied.substring(i, i+FASTA_LINE_LENGTH));
            converted.append("\n");
         }
         converted.append(supplied.substring(i, supplied.length()));
      }
      return converted.toString();
   }

   public static int countLetterInRead(String fasta, String letter) {
      return countLetterInRead(fasta, letter, false);
   }

   public static int countLetterInRead(String fasta, String letter, Boolean caseSensitive) {
      String ungapped = Utils.getUngappedRead(fasta);
      int len = ungapped.length();
      if (len == 0) { return -1; }
   
      int increment = letter.length();
      int count = 0;
      
      for (int i = 0; i <= ungapped.length() - increment; i += increment) {
         if (letter.equals(ungapped.substring(i, i+increment)) && caseSensitive) {
            count++;
         }
         if (letter.equalsIgnoreCase(ungapped.substring(i, i+increment)) && !caseSensitive) {
            count++;
         }
      }
      return count;
   }
   
   public static int[] errorString(int[] s, double readError)
 	{
 		int[] snew = s.clone();
 		
 		Random generator = new Random();
 		for (int iter=0; iter<s.length; iter++)
 		{
 			if (generator.nextDouble()<readError)
 				while(snew[iter]==s[iter])
 					snew[iter] = generator.nextInt(3);
 		}
 		
 		return snew;
 	}

   public static BufferedReader getFile(String fileName, String postfix) throws Exception {
      String[] array = new String[1];
      array[0] = postfix;

      return getFile(fileName, array);
   }
   
 	public static BufferedReader getFile(String fileName, String[] postfix) throws Exception {
       BufferedReader bf = null;

       if (fileName.endsWith("bz2")) {
          // open file as a pipe
          System.err.println("Running command " + "bzip2 -dc " + new File(fileName).getAbsolutePath() + " |");
          Process p = Runtime.getRuntime().exec("bzip2 -dc " + new File(fileName).getAbsolutePath() + " |");
          bf = new BufferedReader(new InputStreamReader(p.getInputStream()));
          System.err.println(bf.ready());
        } else if (fileName.endsWith("gz")) {
          // open file as a pipe
           System.err.println("Runnning comand " + "gzip -dc " + new File(fileName).getAbsolutePath() + " |");
           Process p = Runtime.getRuntime().exec("gzip -dc " + new File(fileName).getAbsolutePath() + " |");
           bf = new BufferedReader(new InputStreamReader(p.getInputStream()));
           System.err.println(bf.ready());
        } else {
           int i = 0;
           for (i = 0; i < postfix.length; i++) {
              if (fileName.endsWith(postfix[i])){
                 bf = new BufferedReader(new FileReader(fileName));
                 break;
              }
           }
           if (i == postfix.length) {
              System.err.println("Unknown file format " + fileName + " Skipping!");
           }
        }

        return bf;
   }
 	
   
   public static String getID(String line) {
      String ids[] = line.split(":");
      int commaPos = ids[1].indexOf(",");
      if (commaPos != -1) {      
         return ids[1].substring(1, commaPos).trim();
      } else {
         return ids[1];
      }
   }
   
   public static double getLetterPercentInRead(String fasta, String letter) {
      int ungappedLen = getUngappedRead(fasta).length();
      int count = countLetterInRead(fasta, letter);
      
      return count / (double)ungappedLen;
   }
   
   public static int getOvlSize(int readA, int readB, int ahang, int bhang) {
      if ((ahang <= 0 && bhang >= 0) || (ahang >= 0 && bhang <= 0)) {
         return -1;
      }
      
      if (ahang < 0) {
         return readA - Math.abs(bhang);
      }
      else {
         return readA - ahang;
      }
   }
   
   public static int getRangeOverlap(int startA, int endA, int startB, int endB) {
      int minA = Math.min(startA, endA);
      int minB = Math.min(startB, endB);
      int maxA = Math.max(startA, endA);
      int maxB = Math.max(startB, endB);
      
      int start = Math.max(minA, minB);
      int end = Math.min(maxA, maxB);
      
      return (end-start+1);
   }
   
   public static String getUngappedRead(String fasta) {
      fasta = fasta.replaceAll("N", "");
      fasta = fasta.replaceAll("-", "");
      
      assert(fasta.length() >= 0);
      
      return fasta;
   }
   
   public static String getValue(String line, String key) {
      if (line.startsWith(key)) {
         return line.split(":")[1];
      }

      return null;
   }

	public static boolean isAContainedInB(int startA, int endA, int startB, int endB) {
      int minA = Math.min(startA, endA);
      int minB = Math.min(startB, endB);
      int maxA = Math.max(startA, endA);
      int maxB = Math.max(startB, endB);

      return (minB < minA && maxB > maxA);
   }

	public static String rc(String supplied) {
      StringBuilder st = new StringBuilder();
      for (int i = supplied.length() - 1; i >= 0; i--) {
         char theChar = supplied.charAt(i);         
         
         if (theChar != '-') {
            Translate t = Translate.valueOf(Character.toString(theChar).toUpperCase());
            st.append(t.getCompliment());
         } else {
            st.append("-");
         }
      }
      return st.toString();
   }

	public static String toProtein(String genome, boolean isReversed, int frame) {
      StringBuilder result = new StringBuilder();

      if (isReversed) {
         genome = rc(genome);
      }
      genome = genome.replaceAll("-", "");
      
      for (int i = frame; i < (genome.length() - 3); i += 3) {
         String codon = genome.substring(i, i+3);
         String protein = ToProtein.valueOf(codon).getProtein();
         result.append(protein);
      }
      
      return result.toString();
   }
}
