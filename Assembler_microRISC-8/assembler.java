import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// VERSAO DO JAVA USADA PARA O DESENVOLVIMENTO DO CÓDIGO
// java 23.0.2 2025-01-21
// Java(TM) SE Runtime Environment (build 23.0.2+7-58)
// Java HotSpot(TM) 64-Bit Server VM (build 23.0.2+7-58, mixed mode, sharing)

public class assembler {

    public static void main(String[] args) throws IOException {

        String inputFile  = "main.asm"; // nome padrão
        String outputFile = "ROM.bin"; // nome padrão
    
        try {
            
            // verifica se foi ou não passado argumentos
            if(args.length == 0) {
                System.err.println("\nEsse comando não é reconhecido pelo Assembler.\n");
                return;
            }

            // Verifica se tem a flag "-help ou -h"
            if (args[0].equals("-help") || args[0].equals("-h")) {
                System.out.println("\nExemplo de uso: \"java -jar assembler.jar -a "+inputFile+"\"");
                System.out.println("\nOpcoes:\n");
                System.out.println("  -a <assembler>    arquivo de entrada (padrao: "+inputFile+")");
                System.out.println("  -h | -help        exibe a mensagem de ajuda\n");
                return;
            }

            // Verifica se tem a flag "-a" e pega o arquivo de entrada
            for (int i = 0; i < args.length; i++) {

                if (args[i].equals("-a") && args.length > i+1) {
                    inputFile = args[i+1];
                }

            }
        
        } catch (Exception e) {
            System.err.println("\nEsse comando não é reconhecido pelo Assembler. Erro: \"" + e.getMessage() + "\"\n");
            return;
        }

        // ------------------------- COMEÇO DA CONVERSÃO DO CODIGO EM ASM PARA BINÁRIO -------------------------

        // Cria um mapa para armazenar os códigos de operação
        Map<String, Integer> tabelaOpcodes = Map.ofEntries(
            Map.entry("LDA",    0b00000000),
            Map.entry("LDI",    0b00000001),
            Map.entry("STA",    0b00000010),
            Map.entry("MOV",    0b00000011),
            Map.entry("ADD",    0b00000100),
            Map.entry("ADD.I",  0b00000101),
            Map.entry("SUB",    0b00000110),
            Map.entry("SUB.I",  0b00000111),
            Map.entry("SHT.L",  0b00001000),
            Map.entry("SHT.R",  0b00001001),
            Map.entry("HLT",    0b00001010),
            Map.entry("JMP",    0b00001011),
            Map.entry("CMP",    0b00001100),
            Map.entry("CMP.I",  0b00001101),
            Map.entry("BEQ",    0b00001110),
            Map.entry("BNE",    0b00001111),
            Map.entry("BLE",    0b00010000),
            Map.entry("BGE",    0b00010001),
            Map.entry("BLT",    0b00010010),
            Map.entry("BGT",    0b00010011),
            Map.entry("BEQ.R",  0b00010100),
            Map.entry("BNE.R",  0b00010101),
            Map.entry("BLE.R",  0b00010110),
            Map.entry("BGE.R",  0b00010111),
            Map.entry("BLT.R",  0b00011000),
            Map.entry("BGT.R",  0b00011001),
            Map.entry("AND",    0b00011010),
            Map.entry("OR",     0b00011011),
            Map.entry("XOR",    0b00011100),
            Map.entry("NOT",    0b00011101),
            Map.entry("END",    0b00011110),
            Map.entry("IN",     0b00011111),
            Map.entry("OUT",    0b00100000),
            Map.entry("CALL",   0b00100001),
            Map.entry("RET",    0b00100010),
            Map.entry("INI.P",  0b00100011),
            Map.entry("SET.P",  0b00100100),
            Map.entry("GET.P",  0b00100101),
            Map.entry("UPD.P",  0b00100110),
            Map.entry("UPI.P",  0b00100111)
        );

        // Cria um mapa para armazenar os registradores
        Map<String, Integer> tabelaRegisters = Map.ofEntries(
            Map.entry("R0",     0b00000000),
            Map.entry("ACC",    0b00000000),
            Map.entry("R1",     0b00000001),
            Map.entry("R2",     0b00000010),
            Map.entry("R3",     0b00000011),
            Map.entry("R4",     0b00000100),
            Map.entry("R5",     0b00000101),
            Map.entry("R6",     0b00000110),
            Map.entry("R7",     0b00000111),
            Map.entry("R8",     0b00001000),
            Map.entry("R9",     0b00001001),
            Map.entry("R10",    0b00001010),
            Map.entry("R11",    0b00001011),
            Map.entry("R12",    0b00001100),
            Map.entry("R13",    0b00001101),
            Map.entry("R14",    0b00001110),
            Map.entry("R15",    0b00001111),
            Map.entry("RZ",     0b00001111) // R15 é o registrador zero (RZ)
        );

        // Cria um mapa para armazenar os ciclos de cada instrução (UPDATE FUTURO PARA LABEL TAMBEM)
        Map<String, Integer> tabCiclos = Map.ofEntries(
            Map.entry("LDA",   4),
            Map.entry("LDI",   3),
            Map.entry("STA",   4),
            Map.entry("MOV",   2),
            Map.entry("ADD",   2),
            Map.entry("ADD.I", 3),
            Map.entry("SUB",   2),
            Map.entry("SUB.I", 3),
            Map.entry("SHT.L", 2),
            Map.entry("SHT.R", 2),
            Map.entry("HLT",   1),
            Map.entry("JMP",   3),
            Map.entry("CMP",   2),
            Map.entry("CMP.I", 3),
            Map.entry("BEQ",   3),
            Map.entry("BNE",   3),
            Map.entry("BLE",   3),
            Map.entry("BGE",   3),
            Map.entry("BLT",   3),
            Map.entry("BGT",   3),
            Map.entry("BEQ.R", 2),
            Map.entry("BNE.R", 2),
            Map.entry("BLE.R", 2),
            Map.entry("BGE.R", 2),
            Map.entry("BLT.R", 2),
            Map.entry("BGT.R", 2),
            Map.entry("AND",   2),
            Map.entry("OR",    2),
            Map.entry("XOR",   2),
            Map.entry("NOT",   2),
            Map.entry("END",   1),
            Map.entry("IN",    1),
            Map.entry("OUT",   1),
            Map.entry("CALL",  3),
            Map.entry("RET",   1),
            Map.entry("INI.P", 3),
            Map.entry("SET.P", 2),
            Map.entry("GET.P", 2),
            Map.entry("UPD.P", 2),
            Map.entry("UPI.P", 2)
        );

        // Cria um mapa vazio para armazenar as labels e seus endereços
        Map<String, Integer> tabelaLabels = new HashMap<>();

        // Inicializa o contador de ciclos
        int cicloTotal = 0;

        // Cria uma lista para armazenar as linhas do arquivo de entrada
        List<String> linhas = new ArrayList<>();

        try {

            // Tenta ler o arquivo de entrada .asm
            linhas = Files.readAllLines(Paths.get(inputFile));

        } catch (Exception e) {
            System.err.println("\nArquivo de entrada não encontrado. Erro: \"" + e.getMessage()+"\"\n");
            return;
        }

        // Cria um conjunto de opcodes que ocupam dois ciclos
        Set<String> opcodesDoisCiclos = Set.of(
            "MOV", "ADD", "SUB", "SHF.L", "SHF.R", "CMP", "AND", "OR", "XOR", "NOT"
        );

        // Inicializa o verificador de dois ciclos
        boolean is_in_two_cycles = false;
        
        // junção dos registradores da operacao de 2 ciclos (-1 para indicar que ainda nÃ£o foi definido)
        int registradores = -1;

        // Cria um arquivo de saí­da .bin
        FileOutputStream output = new FileOutputStream(outputFile);

        // padrao da labels e opcodes (ex: label: e LDA)
        Pattern patternLabels = Pattern.compile("\\b([A-Za-z0-9_]+):|\\w+(?:\\.\\w+)?");

        // padrao dos tokens (ex: LDA R0, #5, &10, 'a', R1.R2, etc)
        Pattern patternTokens = Pattern.compile("'[^']'|&\\d+|#-?\\d+|\\w+:|\\w+(?:\\.\\w+)?");

        System.out.println();
        System.out.println("-------------------- Labels Encontradas --------------------");
        System.out.println();

        // percorre o arquivo em busca de labels
        for (String linha : linhas) {

            // Remove tudo após o ponto e vírgula (comentário)
            linha = linha.split(";", 2)[0];

            // verifica se é ou nao comentário e se a linha não é vazia
            if(!linha.isEmpty() && !linha.startsWith(";")) { // ignora linhas vazias e comentários

                Matcher matcher = patternLabels.matcher(linha);

                while (matcher.find()) {

                    // verifica se é um label
                    if(matcher.group().matches("\\b([A-Za-z0-9_]+):")){
                        String label = matcher.group(1).toUpperCase();
                        tabelaLabels.put(label, cicloTotal); // Adiciona a label e o ciclo atual no mapa de labels
                        System.out.println("LABEL -> \""+label+"\" | ENDERECO -> " +cicloTotal); // Exibe a label e o ciclo atual
                        break;
                    }
                    // verifica se é um opcode
                    if(tabCiclos.containsKey(matcher.group())){
                        cicloTotal += tabCiclos.get(matcher.group().toUpperCase()); // Adiciona o número de ciclos da instrução
                        break;
                    }
                    
                } 

            }  

        }

        System.out.println();

        System.out.println("---------------------- Assembly Lido -----------------------");
        System.out.println();

        cicloTotal = 0; // Reseta o contador de ciclos para a segunda passagem

        // percorre o arquivo em busca de opcodes e registradores
        for(String linha : linhas) {

            // Remove tudo após o ponto e vírgula (comentário)
            linha = linha.split(";", 2)[0];

            // Remove ví­rgula e espaços duplicados
            linha = linha.replace(",", "").trim();

            // Divide a linha por espaços múltiplos, verifica se é ou nao comentário e se a linha não é vazia
            if (!linha.isEmpty() && !linha.startsWith(";")) {
                
                Matcher token = patternTokens.matcher(linha);

                while (token.find()) {

                    // exibe o número de ciclos da instrução
                    if(tabCiclos.containsKey(token.group().toUpperCase())) {
                        System.out.printf("("+cicloTotal+") ");
                        cicloTotal += tabCiclos.get(token.group()); // Adiciona o número de ciclos da instrução
                    }

                    // --------------- Converte os tokens para binário e escreve no arquivo de saí­da ---------------

                    // Trata o token de opcode e verifica se é ou nao de 2 ciclos
                    if(tabelaOpcodes.containsKey(token.group().toUpperCase())) {

                        System.out.printf(token.group()+" ");
                        output.write(tabelaOpcodes.get(token.group().toUpperCase())); // Escreve o opcode no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente

                        if(opcodesDoisCiclos.contains(token.group().toUpperCase())){
                            is_in_two_cycles = true; // Define que a instrução é de dois ciclos
                        }else{
                            is_in_two_cycles = false; // Define que a instrução é de três ciclos
                        }

                    // Trata os registradores de dois ciclos
                    } else if(tabelaRegisters.containsKey(token.group().toUpperCase()) && is_in_two_cycles){

                        // Se não tiver o primeiro registrador, só adicionamos
                        if (registradores == -1) {
                            System.out.printf(token.group().toUpperCase()+" "); // Exibe o registrador
                            registradores = tabelaRegisters.get(token.group().toUpperCase()) & 0x0F; // Pega o registrador e aplica a máscara de 4 bits;

                        } else {

                            System.out.printf(token.group().toUpperCase()+" "); // Exibe o registrador

                            // Se já tiver o primeiro registrador, deslocamos o primeiro e juntamos ao segundo
                            int combinado = (registradores << 4) | tabelaRegisters.get(token.group().toUpperCase()) & 0x0F;
                            output.write(combinado); // Escreve o registrador combinado no arquivo de saí­da
                            output.flush();
                
                            registradores = -1; // Reseta para a próxima instrução
                        }
                        
                    // Trata os registradores de tres ciclos
                    } else if(tabelaRegisters.containsKey(token.group().toUpperCase()) && !is_in_two_cycles){

                        System.out.printf(token.group().toUpperCase()+" ");
                        output.write(tabelaRegisters.get(token.group().toUpperCase())); // Escreve o registrador no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente

                    // Trata os rnumeros inteiros
                    } else if(token.group().matches("#-?\\d+")) {

                        String numero = token.group().substring(1);  // Isso trata tambem numeros negativos
                        int num8bits = Integer.parseInt(numero) & 0xFF; // converte para inteiro e aplica máscara de 8 bits
                        System.out.printf(token.group()+" ");
                        output.write(num8bits); // Escreve o numero no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente

                    // Trata os endereços de memoria
                    } else if (token.group().matches("&\\d+")) { // identifica o "&"

                        String numero = token.group().substring(1); // remove o "&"
                        int num16bits = Integer.parseInt(numero);
                        System.out.printf(token.group()+" ");

                        output.write( ((num16bits >> 8) & 0xFF) ); // Escreve os 8 bits mais significativos no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente
                        output.write( (num16bits & 0xFF) ); // Escreve os 8 bits menos significativos no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente

                    // Trata os caracteres
                    } else if (token.group().matches("^'(.)'$")) { // identifica qualquer caractere (incluindo espaço) entre aspas simples
                        char caractere = token.group().charAt(1); // pega o caractere dentro das aspas simples
                        int num8bits = (int) caractere & 0xFF; // converte para inteiro e aplica máscara de 8 bits
                        System.out.printf(token.group()+" ");
                        output.write(num8bits); // Escreve o numero no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente
                    
                    // Trata labels como endereco de memoria
                    } else if(!token.group().toUpperCase().endsWith(":") && tabelaLabels.containsKey(token.group().toUpperCase())) { // verifica se é uma label sem ":"
                        int endereco16bits = tabelaLabels.get(token.group().toUpperCase()); // Pega o endereço da label
                        System.out.printf(token.group().toUpperCase()+" ("+tabelaLabels.get(token.group().toUpperCase())+") ");
                        output.write( ((endereco16bits >> 8) & 0xFF) ); // Escreve os 8 bits mais significativos no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente
                        output.write( (endereco16bits & 0xFF) ); // Escreve os 8 bits menos significativos no arquivo de saí­da
                        output.flush(); // Garante que o byte seja escrito imediatamente

                    } else if(token.group().toUpperCase().endsWith(":")) {
                        System.out.printf("\n"+token.group().toUpperCase()+" (Não é inserido) "); // Exibe a label com ":"

                    } else {
                        System.out.println("Erro: token desconhecido: "+token.group().toUpperCase()+"\n");
                    }

                }

                // QUEBRA DE LINHA
                System.out.println();

            }

        }

        System.out.println("\n");
        System.out.println("------------------------------------------------------------");

        // Fecha o arquivo de saí­da
        try {
            output.close();
            System.out.println("\nArquivo de saída " + outputFile + " criado com sucesso!");
        } catch (IOException e) {
            System.out.println("\nErro ao fechar o arquivo de saída " + outputFile + ": " + e.getMessage());
        }

        System.out.println("\n");

    }

}