package com.example.frontend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.cloud.vision.v1.*;

@RefreshScope
@Controller
@SessionAttributes( "name" )
public class FrontendController
{
    @Value( "${greeting:Hello}" )
    private String greeting;

    @Autowired
    private GuestbookMessagesClient client;

    @Autowired
    private PubSubTemplate pubSubTemplate;

    @Autowired
    private OutboundGateway outboundGateway;

    // The ApplicationContext is needed to create a new Resource.
    @Autowired
    private ApplicationContext context;

    // Get the Project ID, as its Cloud Storage bucket name here
    @Autowired
    private GcpProjectIdProvider projectIdProvider;

    @Autowired
    private ImageAnnotatorClient annotatorClient;

    private void analyzeImage(String uri) {
        // After the image was written to GCS,
        // analyze it with the GCS URI.It's also
        // possible to analyze an image embedded in
        // the request as a Base64 encoded payload.
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ImageSource imgSrc = ImageSource.newBuilder()
                                        .setGcsImageUri(uri).build();
        Image img = Image.newBuilder().setSource(imgSrc).build();
        Feature feature = Feature.newBuilder()
                                 .setType(Feature.Type.LABEL_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest
                                               .newBuilder()
                                               .addFeatures(feature)
                                               .setImage(img)
                                               .build();
        requests.add(request);
        BatchAnnotateImagesResponse responses =
                annotatorClient.batchAnnotateImages(requests);
        // We send in one image, expecting just
        // one response in batch
        AnnotateImageResponse response =responses.getResponses(0);
        System.out.println(response);
    }

    @GetMapping( "/" )
    public String index( Model model )
    {
        if( model.containsAttribute( "name" ) )
        {
            String name = ( String ) model.asMap().get( "name" );
            model.addAttribute( "greeting", String.format( "%s %s", greeting, name ) );
        }
        model.addAttribute( "messages", client.getMessages().getContent() );
        return "index";
    }

    @PostMapping( "/post" )
    public String post(
            @RequestParam( name = "file", required = false ) MultipartFile file,
            @RequestParam String name,
            @RequestParam String message,
            Model model ) throws IOException
    {
        model.addAttribute( "name", name );

        /*store in bucket*/
        String filename = null;
        if( file != null && !file.isEmpty()
                    && file.getContentType().equals( "image/jpeg" ) )
        {
            // Bucket ID is our Project ID
            String bucket = "gs://" + projectIdProvider.getProjectId();
            // Generate a random file name
            filename = UUID.randomUUID().toString() + ".jpg";
            WritableResource resource = ( WritableResource ) context.getResource( bucket + "/" + filename );

            // Write the file to Cloud Storage
            try( OutputStream os = resource.getOutputStream() )
            {
                os.write( file.getBytes() );
            }

            // After written to GCS, analyze the image.
            analyzeImage(bucket + "/" + filename);
        }

        if( message != null && !message.trim().isEmpty() )
        {

            /*Direct publish to Pub/Sub*/
//			pubSubTemplate.publish( "messages", name + ": " + message );

            /*Abstract publish to Pub/Sub*/
            outboundGateway.publishMessage( name + ": " + message );

            // Post the message to the backend service
            GuestbookMessage payload = new GuestbookMessage();
            payload.setName( name );
            payload.setMessage( message );
            // Store the generated file name in the database
            payload.setImageUri( filename );
            client.add( payload );
        }
        return "redirect:/";
    }

	// ".+" is necessary to capture URI with filename extension
	@GetMapping("/image/{filename:.+}")
	public ResponseEntity<Resource> file(
			@PathVariable String filename) {
		String bucket = "gs://" +
								projectIdProvider.getProjectId();
		// Use "gs://" URI to construct
		// a Spring Resource object
		Resource image = context.getResource(bucket +
													 "/" + filename);
		// Send it back to the client
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_JPEG);
		return new ResponseEntity<>(
				image, headers, HttpStatus.OK);
	}
}

