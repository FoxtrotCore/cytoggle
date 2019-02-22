
package com.foxtrotfanatics.cytoggle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

/**
 * Discord Bot to Toggle Cytube Server through Discord Commands
 * @author Christian77777
 */
public class Cytoggle implements IListener<ReadyEvent>
{
	public static final long[] ROLE_WHITELIST =
	{ 360937988265345036L };
    //    ^ Bot-Access           ^ ADMIN
	public static final String ABSOLUTE_PATH_TO_SCRIPT_PARENT_DIRECTORY = "/usr/bin";
	public static final String ABSOLUTE_PATH_TO_LOG = "/usr/log/cytube-monitor.log";
	public static final String SCRIPT_NAME = "cytube-toggle";
	public static final String TOKEN_ENVIROMENT_VARIABLE_KEY = "CYTOGGLE_TOKEN";
	
	public IRole[] roles;

	/**
	 * Starts Bot with Discord Token, register's listener to add Single Command after ReadyEvent
	 * @param token Discord Token for Login to Gateway
	 */
	public Cytoggle(String token)
	{
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(token);
		clientBuilder.registerListener(this);
		try
		{
			clientBuilder.login();
		}
		catch (DiscordException e)
		{
			System.out.println("Failed To Connect: Probably a bad Discord Token\n" + e);
			System.exit(21);
		}
	}

	/**
	 * Starts Bot with Token from Environment Variable
	 * @param args ignored arguments from command line
	 */
	public static void main(String[] args)
	{
		@SuppressWarnings("unused")
		Cytoggle cytoggle = new Cytoggle(System.getenv(TOKEN_ENVIROMENT_VARIABLE_KEY));
	}

	@Override
	/**
	 * (non-Javadoc)
	 * @see sx.blah.discord.api.events.IListener#handle(sx.blah.discord.api.events.Event)
	 */
	public void handle(ReadyEvent event)
	{
		Thread.currentThread().setName("Command-Initalizer");
		try
		{
			System.out.println("Arbitrarily Waiting for Completion");
			Thread.sleep(10000);
			System.out.println("Trying Installation Now");
		}
		catch (InterruptedException e3)
		{
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		List<IRole> allRoles = event.getClient().getRoles();
		String allRoleListed = "List of all visible Roles:\n";
		for(int x = 0; x < allRoles.size(); x++)
		{
			allRoleListed += allRoles.get(x).getLongID() + " - " + allRoles.get(x).getName() + "\n";
		}
		System.out.println(allRoleListed + "End List");
		//Specify Role whitelist
		roles = new IRole[ROLE_WHITELIST.length];
		System.out.println("RoleIDs to honor: ");
		for (int x = 0; x < roles.length; x++)
		{
			System.out.println("RoleID " + (x+1) + ": " + ROLE_WHITELIST[x]);
			IRole r = event.getClient().getRoleByID(ROLE_WHITELIST[x]);
			if(r == null)
			{
				System.out.println("ERROR: RoleID \"" + ROLE_WHITELIST[x] + "\"is not a viewable Role!");
				System.exit(20);
			}
			System.out.println("Found Role " + (x+1) + ": " + r.getName());
			roles[x] = r;
		}
		System.out.println("Whitelist Built");
		//Install Command
		event.getClient().getDispatcher().registerListener(new IListener<MessageReceivedEvent>()
		{
			@Override
			public void handle(MessageReceivedEvent e)
			{
				//Only care if command at all, easiest computation
				if (e.getMessage().getContent().startsWith(SCRIPT_NAME))
				{
					System.out.println("Prefix Recognized");
					boolean authorized = false;
					for (IRole r : roles)
					{
						if (e.getMessage().getAuthor().hasRole(r))//Authorized
						{
							System.out.println("Role " + r.getName() + " Authorized");
							authorized = true;
							break;
						}
					}
					if (authorized)
					{
						String content = e.getMessage().getContent();
						//Split only Arguments into array, accidently includes empty string occasionally
						String[] arguments = content.substring(13, content.length()).split("\\s+");
						System.out.println("Arguments Found: " + arguments);
						boolean acceptAutomaton = true;
						boolean hasArgument = false;
						for (int x = 0; x < arguments.length; x++)
						{
							String arg = arguments[x];
							//Account for Empty String, ignore
							boolean justBlank = arg.equals("");
							//Check if Flag
							boolean goodFlag = arg.startsWith("-") && arg.length() == 2;
							//Check if required Flag, to execute command at all
							boolean goodArgument = (x == arguments.length - 1 && (arg.equals("on") || arg.equals("off")));
							if (!justBlank && !goodFlag && !goodArgument)
							{
								System.out.println("Rejected on flag: [" + arg + "]");
								RequestBuffer.request(() -> {
									e.getChannel()
											.sendMessage("**Bad Syntax**\nUsage: `cytube-toggle [options] [on/off]`\r\n"
													+ "        Specify `on` or `off` as a token to determine turning the cytube server on or off\r\n"
													+ "        -[k/K]: Keep all extra files generated by the program [Default: false]\r\n"
													+ "        -[t/T]: Trigger all of the webhooks for debugging\r\n"
													+ "        -[v/V]: Enables verbose information mode [Default: false]\r\n"
													+ "        -[w/W]: Disables the webhook triggers sent to discord [Default: false]");
								});
								acceptAutomaton = false;
								break;
							}
							else if (goodArgument)
								hasArgument = true;
						}
						//Execute Command
						if (acceptAutomaton && hasArgument)
						{
							try
							{
								System.out.println("Running Command: " + ABSOLUTE_PATH_TO_SCRIPT_PARENT_DIRECTORY + File.separator + content);
								//"content" var includes script name with args
								Process p = Runtime.getRuntime().exec(ABSOLUTE_PATH_TO_SCRIPT_PARENT_DIRECTORY + File.separator + content);
								System.out.println("Executed...");
								p.waitFor();
								System.out.println("Completed....");
								int exitCode = p.exitValue();
								//Execution Failed
								if (exitCode > 1 || exitCode < 0)
								{
									System.out.println("Error in boot sequence! Uploading Log...");
									File log = new File(ABSOLUTE_PATH_TO_LOG);
									RequestBuffer.request(() -> {
										e.getClient().changePresence(StatusType.IDLE);
									});
									FileNotFoundException ex = RequestBuffer.request(() -> {
										try
										{
											e.getChannel().sendFile("Error Code: " + exitCode, log);
											return null;
										}
										catch (FileNotFoundException e2)
										{
											return e2;
										}
									}).get();
									if (ex != null)
									{
										System.out.println("Could Not Even find Log file");
										RequestBuffer.request(() -> {
											e.getChannel().sendMessage("Error Code: " + exitCode + "\nCould Not Find Log to Upload");
										});
									}
								}
								//Execution Successful
								else
								{
									System.out.println("Execution Successful");
									if(content.endsWith("off"))
										RequestBuffer.request(() -> {
											e.getClient().changePresence(StatusType.DND);
										});
									else
										RequestBuffer.request(() -> {
											e.getClient().changePresence(StatusType.ONLINE);
										});
									RequestBuffer.request(() -> {
										e.getChannel().sendMessage("Done");
									});
								}
							}
							catch (IOException e1)//Likely if command not found, or bad File Path Syntax
							{
								RequestBuffer.request(() -> {
									e.getClient().changePresence(StatusType.IDLE);
								});
								RequestBuffer.request(() -> {
									e.getChannel().sendMessage(e1.getMessage());
								});
								e1.printStackTrace();
							}
							catch (InterruptedException e1)//Shouldn't happen
							{
								RequestBuffer.request(() -> {
									e.getClient().changePresence(StatusType.IDLE);
								});
								e1.printStackTrace();
							}
						}
						//Good Syntax, but does not have required argument
						else if (acceptAutomaton && !hasArgument)
						{
							System.out.println("Bad Syntax in command prefix");
							RequestBuffer.request(() -> {
								e.getChannel()
										.sendMessage("**Bad Syntax**: needs `yes` or `no` argument\nUsage: `cytube-toggle [options] [on/off]`\r\n"
												+ "        Specify `on` or `off` as a token to determine turning the cytube server on or off\r\n"
												+ "        -[k/K]: Keep all extra files generated by the program [Default: false]\r\n"
												+ "        -[t/T]: Trigger all of the webhooks for debugging\r\n"
												+ "        -[v/V]: Enables verbose information mode [Default: false]\r\n"
												+ "        -[w/W]: Disables the webhook triggers sent to discord [Default: false]");
							});
						}
					}
					else
					{
						System.out.println("User not Authorized");
						RequestBuffer.request(() -> {
							e.getAuthor().getOrCreatePMChannel().sendMessage("Not Authorized");
						});
					}

				}
			}
		});
		System.out.println("Command Installed");
		RequestBuffer.request(() -> {
			event.getClient().changePresence(StatusType.DND);
		});
		System.out.println("Listening for \"" + SCRIPT_NAME + "\" now");
	}
}