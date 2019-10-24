package controllers;

import models.data.IProject;
import models.data.Project;
import models.member.IMember;
import models.member.Member;
import models.task.ITask;
import models.task.Task;
import repositories.ProjectRepository;
import util.ParserHelper;
import util.factories.MemberFactory;
import util.factories.TaskFactory;
import util.log.DukeLogger;
import views.CLIView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Scanner;

public class ProjectInputController implements IController {
    private Scanner manageProjectInput;
    private ProjectRepository projectRepository;
    private CLIView consoleView;
    private MemberFactory memberFactory;

    /**
     * Constructor for ProjectInputController takes in a View model and a ProjectRepository.
     * ProjectInputController is responsible for handling user input when user chooses to manage a project.
     * @param consoleView The main UI of ArchDuke.
     * @param projectRepository The object holding all projects.
     */
    public ProjectInputController(CLIView consoleView, ProjectRepository projectRepository) {
        this.manageProjectInput = new Scanner(System.in);
        this.projectRepository = projectRepository;
        this.consoleView = consoleView;
        this.memberFactory = new MemberFactory();
    }

    /**
     * Allows the user to manage the project by branching into the project of their choice.
     * @param input User input containing project index number (to add to project class).
     */
    public void onCommandReceived(String input) {
        DukeLogger.logInfo(ProjectInputController.class, "Managing project: " + input);
        int projectNumber = Integer.parseInt(input);
        Project projectToManage = projectRepository.getItem(projectNumber);
        this.consoleView.consolePrint("Now managing: " + projectToManage.getDescription());
        boolean isManagingAProject = true;
        while (isManagingAProject) {
            isManagingAProject = manageProject(projectToManage);
        }
    }

    /**
     * Manages the project.
     * @param projectToManage The project specified by the user.
     * @return Boolean variable giving status of whether the exit command is entered.
     */
    public boolean manageProject(Project projectToManage) {
        boolean isManagingAProject = true;
        if (manageProjectInput.hasNextLine()) {
            String projectFullCommand = manageProjectInput.nextLine();
            DukeLogger.logInfo(ProjectInputController.class, "Managing:"
                    + projectToManage.getDescription() + ",input:'"
                    + projectFullCommand + "'");
            if (projectFullCommand.matches("exit")) {
                isManagingAProject = projectExit(projectToManage);
            } else if (projectFullCommand.matches("add member.*")) {
                projectAddMember(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("edit member.*")) {
                projectEditMember(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("delete member.*")) {
                projectDeleteMember(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("view members.*")) {
                projectViewMembers(projectToManage);
            } else if (projectFullCommand.matches("role.*")) {
                projectRoleMembers(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("view credits.*")) {
                projectViewCredits(projectToManage);
            } else if (projectFullCommand.matches("add task.*")) {
                projectAddTask(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("view tasks.*")) {
                projectViewTasks(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("view assignments.*")) {
                projectViewAssignments(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("view task requirements i/.*")) { // need to refactor this
                projectViewTaskRequirements(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("edit task requirements.*")) {
                projectEditTaskRequirements(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("edit task.*")) {
                projectEditTask(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("delete task.*")) {
                projectDeleteTask(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("assign task.*")) {
                projectAssignTask(projectToManage, projectFullCommand);
            } else if (projectFullCommand.matches("bye")) {
                consoleView.end();
            } else {
                consoleView.consolePrint("Invalid command. Try again!");
            }
        } else {
            consoleView.consolePrint("Please enter a command.");
        }
        return isManagingAProject;
    }

    /**
     * Adds roles to Members in a Project.
     * @param projectToManage : The project specified by the user.
     * @param projectFullCommand : User input.
     */
    public void projectRoleMembers(Project projectToManage, String projectFullCommand) {
        String parsedCommands = projectFullCommand.substring(5);
        String[] commandOptions = parsedCommands.split(" -n ");
        if (commandOptions.length != 2) {
            consoleView.consolePrint("Wrong command format! Please enter role INDEX -n ROLE_NAME");
            return;
        }
        int memberIndex = Integer.parseInt(commandOptions[0]);
        IMember selectedMember = projectToManage.getMembers().getMember(memberIndex);
        selectedMember.setRole(commandOptions[1]);
        consoleView.consolePrint("Successfully changed the role of " + selectedMember.getName() + " to "
                                + selectedMember.getRole() + ".");
    }

    /**
     * Adds a member to the current project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectAddMember(Project projectToManage, String projectCommand) {
        String memberDetails = projectCommand.substring(11);
        int numberOfCurrentMembers = projectToManage.getNumOfMembers();
        memberDetails = memberDetails + " -x " + numberOfCurrentMembers;
        IMember newMember = memberFactory.create(memberDetails);
        if (newMember.getName() != null) {
            projectToManage.addMember((Member) newMember);
            consoleView.addMember(projectToManage, newMember.getDetails());
        } else {
            consoleView.consolePrint("Failed to add member. Please ensure you have entered "
                    + "at least the name of the new member.");
        }
    }

    /**
     * Updates the details of a given member in the current project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectEditMember(Project projectToManage, String projectCommand) {
        try {
            int memberIndexNumber = Integer.parseInt(projectCommand.substring(12).split(" ")[0]);
            if (projectToManage.getNumOfMembers() >= memberIndexNumber && memberIndexNumber > 0) {
                String updatedMemberDetails = projectCommand.substring(projectCommand.indexOf("-"));
                consoleView.editMember(projectToManage, memberIndexNumber, updatedMemberDetails);
            } else {
                consoleView.consolePrint("The member index entered is invalid.");
            }
        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            consoleView.consolePrint("Please enter the updated member details format correctly.");
        }
    }

    /**
     * Deletes a member from the current project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectDeleteMember(Project projectToManage, String projectCommand) {
        int memberIndexNumber = Integer.parseInt(projectCommand.substring(14).split(" ")[0]);
        if (projectToManage.getNumOfMembers() >= memberIndexNumber) {
            Member memberToRemove = projectToManage.getMembers().getMember(memberIndexNumber);
            projectToManage.removeMember(memberToRemove);
            consoleView.consolePrint("Removed member with the index number " + memberIndexNumber);
        } else {
            consoleView.consolePrint("The member index entered is invalid.");
        }
    }

    /**
     * Displays all the members in the current project.
     * @param projectToManage The project specified by the user.
     */
    public void projectViewMembers(Project projectToManage) {
        consoleView.viewAllMembers(projectToManage);
    }

    /**
     * Displays the members’ credits, their index number, name, and name of tasks completed.
     * @param projectToManage The project specified by the user.
     */
    public void projectViewCredits(IProject projectToManage) {
        // TODO view all credits.
        consoleView.viewCredits(projectToManage);
        consoleView.consolePrint("Not implemented yet");
    }

    /**
     * Adds a task to the current project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectAddTask(Project projectToManage, String projectCommand) {
        try {
            TaskFactory taskFactory = new TaskFactory();
            ITask newTask = taskFactory.createTask(projectCommand.substring(9));
            if (newTask.getDetails() != null) {
                consoleView.addTask(projectToManage, (Task) newTask);
            } else {
                consoleView.consolePrint("Failed to create new task. Please ensure all "
                        + "necessary parameters are given");
            }
        } catch (NumberFormatException | ParseException e) {
            consoleView.consolePrint("Please enter your task format correctly.");
        }
    }

    /**
     * Updates the task details of a given task in the project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectEditTask(Project projectToManage, String projectCommand) {
        try {
            int taskIndexNumber = Integer.parseInt(projectCommand.substring(10).split(" ")[0]);
            String updatedTaskDetails = projectCommand.substring(projectCommand.indexOf("-"));

            if (projectToManage.getNumOfTasks() >= taskIndexNumber && taskIndexNumber > 0) {
                consoleView.editTask(projectToManage, updatedTaskDetails, taskIndexNumber);
            } else {
                consoleView.consolePrint("The task index entered is invalid.");
            }
        } catch (NumberFormatException e) {
            consoleView.consolePrint("Please enter your task format correctly.");
        }
    }

    /**
     * Deletes a task from the project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectDeleteTask(Project projectToManage, String projectCommand) {
        int taskIndexNumber = Integer.parseInt(projectCommand.substring(12).split(" ")[0]);
        if (projectToManage.getNumOfTasks() >= taskIndexNumber) {
            consoleView.removeTask(projectToManage, taskIndexNumber);
        } else {
            consoleView.consolePrint("The task index entered is invalid.");
        }
    }

    /**
     * Updates the task requirements of a given task in the project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectEditTaskRequirements(Project projectToManage, String projectCommand) {
        String[] updatedTaskRequirements = projectCommand.split(" [ir]m?/");
        int taskIndexNumber = Integer.parseInt(updatedTaskRequirements[1]);
        boolean haveRemove = false;
        if (projectCommand.contains(" rm/")) {
            haveRemove = true;
        }
        if (projectToManage.getNumOfTasks() >= taskIndexNumber && taskIndexNumber > 0) {
            consoleView.editTaskRequirements(projectToManage, taskIndexNumber, updatedTaskRequirements,
                    haveRemove);
        } else {
            consoleView.consolePrint("The task index entered is invalid.");
        }
    }

    /**
     * Displays the tasks in the current project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectViewTaskRequirements(Project projectToManage, String projectCommand) {
        int taskIndex = Integer.parseInt(projectCommand.substring(25));
        if (projectToManage.getNumOfTasks() >= taskIndex && taskIndex > 0) {
            if (projectToManage.getTask(taskIndex).getNumOfTaskRequirements() == 0) {
                consoleView.consolePrint("This task has no specific requirements.");
            } else {
                consoleView.viewTaskRequirements(projectToManage, taskIndex);
            }
        } else {
            consoleView.consolePrint("The task index entered is invalid.");
        }
    }

    /**
     * Manages the assignment to and removal of tasks from members.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectAssignTask(Project projectToManage, String projectCommand) {
        AssignmentController assignmentController = new AssignmentController(projectToManage);
        assignmentController.assignAndUnassign(projectCommand.substring(12));
        consoleView.consolePrint(assignmentController.getErrorMessages().toArray(new String[0]));
        consoleView.consolePrint(assignmentController.getSuccessMessages().toArray(new String[0]));
    }

    /**
     * Displays list of assignments according to specifications of user.
     * @param projectToManage The project to manage.
     * @param projectFullCommand The full command by the user.
     */
    private void projectViewAssignments(Project projectToManage, String projectFullCommand) {
        String input = projectFullCommand.substring(18);
        if (input.charAt(0) == 'm') {
            projectViewMembersAssignments(projectToManage, projectFullCommand.substring(20));
        } else if (input.charAt(0) == 't') {
            projectViewTasksAssignments(projectToManage, projectFullCommand.substring(20));
        }
    }

    /**
     * Displays the assigned tasks in the current project.
     * @param assignedTaskList The list containing the assignment of the tasks.
     */
    public void projectViewAssignedTasks(ArrayList<String> assignedTaskList) {
        consoleView.consolePrint(assignedTaskList.toArray(new String[0]));
    }

    /**
     * Displays all the tasks in the given project.
     * @param projectToManage The project specified by the user.
     * @param projectCommand The user input.
     */
    public void projectViewTasks(Project projectToManage, String projectCommand) {
        if (("view tasks").equals(projectCommand)) {
            consoleView.viewAllTasks(projectToManage);
        } else if (projectCommand.length() >= 11) {
            String sortCriteria = projectCommand.substring(11);
            consoleView.viewSortedTasks(projectToManage, sortCriteria);
        }
    }

    /**
     * Prints a list of members' individual list of tasks.
     * @param projectToManage the project being managed.
     * @param projectCommand The command by the user containing index numbers of the members to view.
     */
    public void projectViewMembersAssignments(Project projectToManage, String projectCommand) {
        ParserHelper parserHelper = new ParserHelper();
        ArrayList<Integer> validMembers = parserHelper.parseMembersIndexes(projectCommand,
            projectToManage.getNumOfMembers());
        if (!parserHelper.getErrorMessages().isEmpty()) {
            consoleView.consolePrint(parserHelper.getErrorMessages().toArray(new String[0]));
        }
        consoleView.consolePrint(AssignmentViewHelper.getMemberOutput(validMembers,
            projectToManage).toArray(new String[0]));
    }

    /**
     * Prints a list of tasks and the members assigned to them.
     * @param projectToManage The project to manage.
     * @param projectCommand The user input.
     */
    private void projectViewTasksAssignments(Project projectToManage, String projectCommand) {
        ParserHelper parserHelper = new ParserHelper();
        ArrayList<Integer> validTasks = parserHelper.parseTasksIndexes(projectCommand,
            projectToManage.getNumOfTasks());
        if (!parserHelper.getErrorMessages().isEmpty()) {
            consoleView.consolePrint(parserHelper.getErrorMessages().toArray(new String[0]));
        }
        consoleView.consolePrint(AssignmentViewHelper.getTaskOutput(validTasks,
            projectToManage).toArray(new String[0]));
    }


    /**
     * Exits the current project.
     * @param projectToManage The project specified by the user.
     * @return Boolean variable specifying the exit status.
     */
    public boolean projectExit(Project projectToManage) {
        boolean isManagingAProject;
        isManagingAProject = false;
        consoleView.exitProject(projectToManage.getDescription());
        return isManagingAProject;
    }

}
