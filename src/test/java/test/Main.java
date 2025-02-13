package test;

import com.amiano4.httpflux.HttpService;

public class Main {
	public static void main(String[] args) {

		try {
			System.out.println("Test case 1: ");

			HttpService.get("https://httpbin.org/post").onSuccess((response) -> {
				System.out.println(response.body());
			}).onError(Throwable::printStackTrace).executeAsync();

			Thread.sleep(3000);

			System.out.println("Test end");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println("Test case 2: ");

		// HttpService.get("https://httpbin.org/get").onSuccess((response) -> {
		// System.out.println(response.body());
		// }).onError((error) -> {
		// // System.out.println(error.getResponse().body());
		// error.printStackTrace();
		// });
	}
}
