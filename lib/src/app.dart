import 'package:flutter/material.dart';

class KaviApp extends StatelessWidget {
  const KaviApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const Placeholder();
  }

  ThemeData themeFrom({required MaterialColor color}) {
    return ThemeData(
      splashColor: color.withOpacity(0.54),
      highlightColor: color.withOpacity(0.36),
      scaffoldBackgroundColor: color.shade50,
      buttonTheme: const ButtonThemeData(
        textTheme: ButtonTextTheme.primary,
        height: 56,
        minWidth: 240,
      ),
      textTheme: Typography.englishLike2018,
      splashFactory: InkRipple.splashFactory,
      bottomAppBarTheme: BottomAppBarTheme(color: color.shade200),
      colorScheme: ColorScheme.fromSwatch(primarySwatch: color)
          .copyWith(secondary: color),
    );
  }
}

var colorPalette = [
  Colors.red,
  Colors.pink,
  Colors.purple,
  Colors.deepPurple,
  Colors.indigo,
  Colors.blue,
  Colors.lightBlue,
  Colors.cyan,
  Colors.teal,
  Colors.green,
  Colors.lightGreen,
  Colors.lime,
  Colors.yellow,
  Colors.amber,
  Colors.orange,
  Colors.deepOrange,
  Colors.brown,
  Colors.grey,
  Colors.blueGrey
];
