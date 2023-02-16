package rest;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import businesslogic.TransactionIdentificator;
import businesslogic.TransactionIdentificator2;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpCode;
import utilities.Constants;
import utilities.IOUtilities;

/**
 * Clase que actúa como controlador del microservicio.
 * @author Equipo de desarrollo de Switch Capa de Integración - Ecosistemas digitales
 * Líder Técnico: Wilson Martinez.
 */
public class CurrentOrNextDayRestController {
    private Integer defaultServerPort = 9001;
    private static final String ENDPOINT_MAIN = "";
    private static final String ENDPOINT_CURRENT_NEXTDAY = "/clasificar-current-o-nextday";
    private static String CUSTOMIZED_ENDPOINT_CURRENT_NEXTDAY = "" + ENDPOINT_CURRENT_NEXTDAY;
    private static final String ENDPOINT_STATUS = "/status";
    private final String URL_EXTERNAL_API = "http://localhost:4444";
    private Map<String, Object> statusInformation = new ConcurrentHashMap<>();
    private java.time.LocalDateTime elapsedDateTime;
    private AtomicInteger succesfulTransactionClassifications = new AtomicInteger(0);
    private AtomicInteger unsuccessfulTransactionClassifications = new AtomicInteger(0);
    private AtomicInteger successfulSetNonWorkingDaysCount = new AtomicInteger(0);
    private AtomicInteger unsuccessfulSetNonWorkingDaysCount = new AtomicInteger(0);
    private static final String CLASSIFICATION_KEY = "CLASSIFICATION_CURRENT_OR_NEXTDAY";
    private static final String MESSAGE_KEY = "MESSAGE";
    private static final String ERROR_MESSAGE_KEY = "ERROR_MESSAGE";
    private static final String MICROSERVICE_STATUS_KEY = "MICROSERVICE_STATUS";
    private static final String MICROSERVICE_IS_ABLE_TO_OPERATE_KEY = "MICROSERVICE_IS_ABLE_TO_OPERATE";
    private boolean microserviceIsAbleToOperate = false;

    /**
     * Método para ejecutar el microservicio.
     * @param serverPort Número de puerto para la ejecución del microservicio.
     * @param customizedEndPoint Cadena de caracteres para personalización del nombre del punto de entrada (endpoint).
     */
    public void loadRestController(Integer serverPort, String customizedEndPoint){
        unsuccessfulSetNonWorkingDaysCount.set(0);
        successfulSetNonWorkingDaysCount.set(0);
        unsuccessfulTransactionClassifications.set(0);
        succesfulTransactionClassifications.set(0);
        Javalin app;
        TransactionIdentificator txIdentificator = new TransactionIdentificator();
        txIdentificator.inicializarDatos();
        TransactionIdentificator2 transactionIdentificator2 = new TransactionIdentificator2();

		//Configuración de número de puerto personalizado para este microservicio.
        if(serverPort!=null && serverPort > 1023 && serverPort < 65536) {
            try{
            	app = Javalin.create().start(serverPort);
            }
            catch(Exception serverPortException) {
            	System.out.println("No es posible utilizar el número de puerto especificado.");
            	try{
                    app = Javalin.create().start();
                }
                catch (Exception busyServerPortException){
                    app = Javalin.create().start(0);// Ejecución del microservicio en un puerto aleatorio.
                }
            }
		}
		else {
			try{
                app = Javalin.create().start();
            }
            catch (Exception javalinAppDefaultPortException){
                app = Javalin.create().start(0);// Ejecución del microservicio en un puerto aleatorio.
            }
		}
        microserviceIsAbleToOperate = true;
        elapsedDateTime = LocalDateTime.now(ZoneId.systemDefault());

        // Endpoint cuyo nombre no es personalizado desde parámetro.
        app.get(ENDPOINT_CURRENT_NEXTDAY + "/{fechaCompensacion}/{fechaYHoraCalendario}",
                ctx -> {
                    String fechaCompensacion = ctx.pathParam("fechaCompensacion");
                    String fechaYHoraCalendario = ctx.pathParam("fechaYHoraCalendario");
                    Map<String, Object> response = new HashMap<>();
                    try{
                        String classification2 = transactionIdentificator2.validateDatesAndclassifytransaction(fechaCompensacion, fechaYHoraCalendario);
                        succesfulTransactionClassifications.incrementAndGet();
                        response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.SUCCESS);
                        response.put(CLASSIFICATION_KEY, classification2);
                        ctx.status(HttpCode.OK)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result( new JSONObject( response ).toString() );
                    }
                    catch (Exception classifytransactionException){
                        unsuccessfulTransactionClassifications.incrementAndGet();
                        response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.FAILED);
                        response.put(ERROR_MESSAGE_KEY, classifytransactionException.getMessage());
                        ctx.status(HttpCode.OK)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result( new JSONObject( response ).toString() );
                    }
                }
        );

        // Personalización del nombre del endpoint a partir del parámetro 'customizedEndPoint'.
        if(customizedEndPoint!=null && customizedEndPoint.length()>1){
            CUSTOMIZED_ENDPOINT_CURRENT_NEXTDAY = customizedEndPoint.startsWith("/") ? customizedEndPoint : "/"+customizedEndPoint;
        }
        // Con el nombre del endpoint personalizado, se construye la segunda versión del anterior endpoint.
        app.get(CUSTOMIZED_ENDPOINT_CURRENT_NEXTDAY,
                ctx -> {
                    String fechaCompensacion = ctx.queryParam("fechaCompensacion");
                    String fechaYHoraCalendario = ctx.queryParam("fechaYHoraCalendario");
                    Map<String, Object> response = new HashMap<>();
                    try{
                        String classification2 = transactionIdentificator2.validateDatesAndclassifytransaction(fechaCompensacion, fechaYHoraCalendario);
                        succesfulTransactionClassifications.incrementAndGet();
                        response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.SUCCESS);
                        response.put(CLASSIFICATION_KEY, classification2);
                        ctx.status(HttpCode.OK)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result( new JSONObject( response ).toString() );
                    }
                    catch (Exception classifytransactionException){
                        unsuccessfulTransactionClassifications.incrementAndGet();
                        response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.FAILED);
                        response.put(ERROR_MESSAGE_KEY, classifytransactionException.getMessage());
                        ctx.status(HttpCode.OK)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result( new JSONObject( response ).toString() );
                    }
                }
        );

        // endpoint para fijar el microservicio en modo 'NEXT_DAY' mediante estado de cierre en 'Verdadero'.
        app.get("/fijar-en-modo-nextday" ,
                ctx -> {
                    Boolean isclosingJourney2 = transactionIdentificator2.isClosedJourney();
                    String message = "Está en cierre 2: " + isclosingJourney2 ;
                    Map<String, Object> response = new HashMap<>();
                    response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.SUCCESS);
                    response.put(MESSAGE_KEY, message);
                    ctx.status(HttpCode.OK)
                            .contentType(ContentType.APPLICATION_JSON)
                            .result( new JSONObject( response ).toString() );
                }
        );

        // endpoint para actualizar en memoria el listado de días festivos.
        app.get("/actualizar-dias-festivos" ,
                ctx -> {
                    HashMap<String, Object> nonWorkingDays;
                    nonWorkingDays = ctx.bodyAsClass(HashMap.class);
                    txIdentificator.setNonWorkingDays(nonWorkingDays);
                    Map<String, Object> response = new HashMap<>();
                    try{
                        transactionIdentificator2.setNonWorkingDays(nonWorkingDays);
                        successfulSetNonWorkingDaysCount.incrementAndGet();
                        response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.SUCCESS);
                        response.put(MESSAGE_KEY, "OK");
                        ctx.status(HttpCode.OK)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result( new JSONObject( response ).toString() );
                    }
                    catch (Exception setNonWorkingDaysException){
                        unsuccessfulSetNonWorkingDaysCount.incrementAndGet();
                        response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.FAILED);
                        response.put(ERROR_MESSAGE_KEY, setNonWorkingDaysException.getMessage());
                        ctx.status(HttpCode.OK)
                                .contentType(ContentType.APPLICATION_JSON)
                                .result( new JSONObject( response ).toString() );
                    }
                }
        );

        // Endpoint para mostrar el estado de este microservicio.
        app.get(ENDPOINT_STATUS,
                ctx -> {
                    statusInformation.put("Microservice elapsed since", elapsedDateTime);
                    statusInformation.put(MICROSERVICE_IS_ABLE_TO_OPERATE_KEY, microserviceIsAbleToOperate ? "Microservicio "+this.getClass().getSimpleName()+" está operando normalmente." : "Microservicio "+this.getClass().getSimpleName()+" no está operando normalmente.");
                    statusInformation.put("succesfulTransactionClassifications", succesfulTransactionClassifications);
                    if(unsuccessfulTransactionClassifications.get()>0) statusInformation.put("unsuccessfulTransactionClassifications", unsuccessfulTransactionClassifications);
                    if(successfulSetNonWorkingDaysCount.get()>0) statusInformation.put("successfulSetNonWorkingDaysCount", successfulSetNonWorkingDaysCount);
                    if(unsuccessfulSetNonWorkingDaysCount.get()>0) statusInformation.put("unsuccessfulSetNonWorkingDaysCount", unsuccessfulSetNonWorkingDaysCount);

                    Map<String, Object> response = new HashMap<>();
                    response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.SUCCESS);
                    response.put(MICROSERVICE_STATUS_KEY, this.statusInformation);
                    ctx.status(HttpCode.OK)
                            .contentType(ContentType.APPLICATION_JSON)
                            .result( new JSONObject( response ).toString() );
                }
        );

        // Endpoint por defecto.
        app.get(ENDPOINT_MAIN,
                ctx -> {
                    Map<String, Object> response = new HashMap<>();
                    String message = "" + CurrentOrNextDayRestController.class.getSimpleName()
                            + "\nIntenta con URL como los siguientes:"
                            + "\n" + ENDPOINT_CURRENT_NEXTDAY + "/{fechaCompensacion}/{fechaYHoraCalendario}"
                            + "\n" + CUSTOMIZED_ENDPOINT_CURRENT_NEXTDAY + "?fechaYHoraCalendario=MMddHHmmSS&fechaCompensacion=MMdd";
                    response.put(Constants.Z_STATUS_KEY, Constants.Z_STATUS_ENUM.SUCCESS);
                    response.put(MESSAGE_KEY, message);
                    ctx.status(HttpCode.OK)
                            .contentType(ContentType.APPLICATION_JSON)
                            .result( new JSONObject( response ).toString() );
                }
        );

    }

    /**
     * Método para conexión a microservicio externo para obtener el listado de días no laborales del año en curso.
     * @return Lista de pares llave-valor con los días no laborales del año en curso.
     */
    private Map<String, Object> connectToExternalService(){
        Map<String, Object> maptoSend = new HashMap<>();
        maptoSend.put("FIELD_003", "401010");
        Map<String, Object> resultMap = new HashMap<>();
        try {
            URL url = new URL(URL_EXTERNAL_API + "/DIAS-FESTIVOS" );
            HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
            JSONObject jsonObject = new JSONObject(IOUtilities.readFullyAsString(urlcon.getInputStream()));
            resultMap = jsonObject.toMap();
        } catch (Exception exception) {
            resultMap.put( "error", "Error recuperando datos desde API externa con IP " + URL_EXTERNAL_API);
        } finally {
            return resultMap;
        }
    }
}
