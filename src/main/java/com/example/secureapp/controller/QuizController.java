package com.example.secureapp.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import com.example.secureapp.service.QuizUserDetailsService;
import com.example.secureapp.model.Questions;
import com.example.secureapp.service.QuestionsService;

@Controller
public class QuizController {

	private final QuizUserDetailsService userDetailsService;
	private final QuestionsService questionsService;
	private final AuthenticationManager authenticationManager;

    public QuizController(QuizUserDetailsService userDetailsService, AuthenticationManager authenticationManager, QuestionsService questionsService) {
        this.userDetailsService = userDetailsService;
		this.authenticationManager = authenticationManager;
		this.questionsService = questionsService;
    }
    
	@GetMapping("/home")
	public String homepage(Model model) {
	    // Get the authenticated user's details
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	    // Get the username
	    String username = authentication.getName();
	    model.addAttribute("username", username);

	    // Get the user's role
	    String role = authentication.getAuthorities().stream()
	        .map(GrantedAuthority::getAuthority)
	        .findFirst()
	        .orElse("ROLE_USER"); // Default role if no authority is found

	    // Redirect to the appropriate page based on the role
	    if (role.equals("ROLE_ADMIN")) {
			// Fetch the latest questions from the service
			List<Questions> questions = questionsService.getQuestionsList();
         model.addAttribute("questions", questions);
        
			
			
	        return "QuizList"; // Return the QuizList.html template
	    } else {
			// Fetch the latest questions from the service
			List<Questions> questions = questionsService.getQuestionsList();
        
			// Add the questions to the model
			model.addAttribute("questions", questions);
	        return "Questions"; // Return the Questions.html template
	    }
	}

	@GetMapping("/login")
    public String login() {
        return "login"; // Returns the login.html template
    }

	@GetMapping("/register")
    public String register() {
        return "register"; // Returns the register.html template
    }

	// POST endpoint to handle user registration and auto-login
	@PostMapping("/register")
	public String registerUser(
			@RequestParam String username,
            @RequestParam String email, // Username from the form
			@RequestParam String password, // Password from the form
			@RequestParam String role // Role from the form
	) {
		// Register the user by storing their details in the HashMap
		try {
			userDetailsService.registerUser(username, password, email, role);
		} catch (Exception userExistsAlready) {
			// Redirect to the /register endpoint
			return "redirect:/register?error";
		}

		// Authenticate the user programmatically
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(username, password)
		);

		// Set the authentication in the SecurityContext
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// Redirect to the /login endpoint
		return "redirect:/login?success";
	}	

	@GetMapping("/addQuestion")
    public String showaddQuestionForm(Model model) {
        model.addAttribute("questions", new Questions()); // Add a new Questions object to the model
        return "addQuestion"; // Return the addQuestion.html template
    }
	@PostMapping("/addQuestion")
public String addQuestion(
        @ModelAttribute Questions questions,
        @RequestParam("options") String options,
        Model model,
        Authentication authentication) {

    // Convert String -> ArrayList<String>
    questions.setOptionsFromString(options);

    String role = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("ROLE_USER");

    if (role.equals("ROLE_ADMIN")) {
        questions.setId(questionsService.getNextId());
        questionsService.addQuestion(questions);
        return "redirect:/home";
    }

    return "redirect:/addQuestion?error";
}

	// Display the edit questions page
    @GetMapping("/editQuestion/{id}")
    public String showEditQuestionForm(@PathVariable("id") int id, Model model) {
        // Find the questions by ID
        Questions questions = questionsService.getQuestionById(id);
        
        // Add the questions to the model
        model.addAttribute("questions", questions);
        
        // Return the editQuestion(questions).html template
        return "editQuestion";
    }

	@PostMapping("/editQuestion")
public String editQuestion(
        @ModelAttribute("questions") Questions questions,
        @RequestParam("options") String options) {

    // Convert String -> ArrayList<String>
    questions.setOptionsFromString(options);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    String role = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("ROLE_USER");

    if (role.equals("ROLE_ADMIN")) {
        questionsService.editQuestion(questions);
    }

    return "redirect:/home";
}

	@GetMapping("/deleteQuestion/{id}")
	public String deleteQuestion(@PathVariable("id") int id, Model model) {
		// Get the authenticated user's details
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		// Get the user's role
		String role = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.findFirst()
			.orElse("ROLE_USER"); // Default role if no authority is found
	
		// Redirect to the appropriate page based on the role
		if (role.equals("ROLE_ADMIN")) {
			// Delete the questions by ID
			questionsService.deleteQuestion(id);
			return "redirect:/home"; // Redirect to the questions list page
		} else {
			return "redirect:/home"; // Redirect to the home page
		}
	}
@PostMapping("/submitQuiz")
public String evaluateQuiz(@RequestParam Map<String, String> allParams, Model model) {
    int correctAnswers = 0;
    List<String> userAnswers = new ArrayList<>();
    ArrayList<Questions> questions = questionsService.getQuestionsList();

    // Iterate through the questions and compare answers
    for (int i = 0; i < questions.size(); i++) {
        String userAnswer = allParams.get("answer" + i); // Get the answer for question i
        userAnswers.add(userAnswer); // Store user's answer
        if (questions.get(i).getCorrectAnswer().equals(userAnswer)) {
            correctAnswers++;
        }
    }

    // Add data to the model
    model.addAttribute("questions", questions);
    model.addAttribute("userAnswers", userAnswers);
    model.addAttribute("correctAnswers", correctAnswers);
    model.addAttribute("totalQuestions", questions.size());

    // Return the result template
    return "result";
}}
