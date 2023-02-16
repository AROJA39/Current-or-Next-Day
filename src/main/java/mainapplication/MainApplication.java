package mainapplication;

import rest.CurrentOrNextDayRestController;

/**
 * Clase ejecutable del proyecto.
 * @author Equipo de desarrollo de Switch Capa de Integración - Ecosistemas digitales - Banco de Bogotá.
 * Líder Técnico: Wilson Martínez.
 */
public class MainApplication {
	/**
	 * Función ejecutable del proyecto.
	 * @param args Parámetros recibidos desde la línea de comandos. En caso de que el primer parámetro sea un número entero, se intentará usar como número de puerto para la ejecución del microservicio; si no, se usará el puerto por defecto (8080).
	 */
	public static void main(String[] args){
		Integer serverPort = null;
		String endPoint = null;
		try {
			serverPort = Integer.parseInt(args[0]);
			endPoint = args[1];
		}
		catch(Exception serverPortException) {;}
        (new CurrentOrNextDayRestController()).loadRestController(serverPort, endPoint);
    }
}
